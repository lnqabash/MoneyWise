package app.moneymate.budget;

import app.moneymate.budget.dto.BudgetDtos.BudgetResponse;
import app.moneymate.budget.dto.BudgetDtos.ExpenseBody;
import app.moneymate.budget.dto.BudgetDtos.IncomeBody;
import app.moneymate.budget.dto.BudgetDtos.SafeToSpendResult;
import app.moneymate.common.events.IncomeRecordedEvent;
import app.moneymate.ledger.LedgerService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class BudgetService {

    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;
    private final SafeToSpendCalculator calculator;
    private final ForecastEngine forecastEngine;
    private final SmoothingEngine smoothingEngine;
    private final ExpenseCategoriser categoriser;
    private final LedgerService ledger;
    private final ApplicationEventPublisher events;

    public BudgetService(IncomeRepository incomes, ExpenseRepository expenses,
                         SafeToSpendCalculator calculator, ForecastEngine forecastEngine,
                         SmoothingEngine smoothingEngine, ExpenseCategoriser categoriser,
                         LedgerService ledger, ApplicationEventPublisher events) {
        this.incomes = incomes;
        this.expenses = expenses;
        this.calculator = calculator;
        this.forecastEngine = forecastEngine;
        this.smoothingEngine = smoothingEngine;
        this.categoriser = categoriser;
        this.ledger = ledger;
        this.events = events;
    }

    @Transactional
    public UUID recordIncome(UUID userId, IncomeBody body) {
        Income income = Income.create(userId, body.amountMinor(), body.currency(), body.source(),
                body.capture() == null ? "manual" : body.capture(), body.isBusiness(), body.receivedOn());
        incomes.save(income);

        // Post to the ledger (single source of truth for money movement).
        ledger.recordIncome(userId, body.amountMinor(), body.currency(), income.getIncomeId());

        // Publish the life-event -> budget re-forecast (Journey A).
        events.publishEvent(new IncomeRecordedEvent(
                userId, income.getIncomeId(), body.amountMinor(), body.currency()));

        return income.getIncomeId();
    }

    @Transactional
    public ExpenseResult recordExpense(UUID userId, ExpenseBody body) {
        var categorised = categoriser.categorise(body.category(), body.note());
        Expense expense = Expense.create(userId, body.amountMinor(), body.currency(), categorised.category(),
                body.capture() == null ? "manual" : body.capture(), body.isBusiness(), body.spentOn());
        expenses.save(expense);

        ledger.recordExpense(userId, body.amountMinor(), body.currency(), expense.getExpenseId());

        return new ExpenseResult(expense.getExpenseId(), categorised.category(), categorised.autoCategorised());
    }

    public record ExpenseResult(UUID expenseId, String category, boolean autoCategorised) {}

    @Transactional(readOnly = true)
    public BudgetResponse currentBudget(UUID userId, String currency) {
        LocalDate windowStart = LocalDate.now().minusDays(56);
        var incomeList = incomes.findByUserIdAndReceivedOnGreaterThanEqual(userId, windowStart);
        var expenseList = expenses.findByUserIdAndSpentOnGreaterThanEqual(userId, windowStart);

        SafeToSpendResult r = calculator.compute(currency, incomeList, expenseList);
        return new BudgetResponse("dynamic_envelope", r.safeToSpendTodayMinor(), r.currency(),
                r.confidence(), r.envelopes(), r.explanation());
    }

    @Transactional(readOnly = true)
    public app.moneymate.budget.dto.BudgetDtos.ForecastResponse forecast(
            UUID userId, String currency, int horizonDays) {
        LocalDate windowStart = LocalDate.now().minusDays(56);
        var incomeList = incomes.findByUserIdAndReceivedOnGreaterThanEqual(userId, windowStart);
        var expenseList = expenses.findByUserIdAndSpentOnGreaterThanEqual(userId, windowStart);
        return forecastEngine.forecast(currency, incomeList, expenseList, horizonDays);
    }

    public app.moneymate.budget.dto.BudgetDtos.SmoothingResponse smooth(
            String currency, long amountMinor, int weeks) {
        return smoothingEngine.smooth(currency, amountMinor, weeks);
    }
}
