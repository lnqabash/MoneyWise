package app.moneymate.budget;

import app.moneymate.budget.dto.BudgetDtos.BudgetResponse;
import app.moneymate.budget.dto.BudgetDtos.ExpenseBody;
import app.moneymate.budget.dto.BudgetDtos.IncomeBody;
import app.moneymate.budget.dto.BudgetDtos.SafeToSpendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class BudgetService {

    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;
    private final SafeToSpendCalculator calculator;

    public BudgetService(IncomeRepository incomes, ExpenseRepository expenses,
                         SafeToSpendCalculator calculator) {
        this.incomes = incomes;
        this.expenses = expenses;
        this.calculator = calculator;
    }

    @Transactional
    public UUID recordIncome(UUID userId, IncomeBody body) {
        Income income = Income.create(userId, body.amountMinor(), body.currency(), body.source(),
                body.capture() == null ? "manual" : body.capture(), body.isBusiness(), body.receivedOn());
        incomes.save(income);
        // Real impl: publish income.recorded to the event bus -> triggers re-forecast.
        return income.getIncomeId();
    }

    @Transactional
    public UUID recordExpense(UUID userId, ExpenseBody body) {
        Expense expense = Expense.create(userId, body.amountMinor(), body.currency(), body.category(),
                body.capture() == null ? "manual" : body.capture(), body.isBusiness(), body.spentOn());
        expenses.save(expense);
        return expense.getExpenseId();
    }

    @Transactional(readOnly = true)
    public BudgetResponse currentBudget(UUID userId, String currency) {
        LocalDate windowStart = LocalDate.now().minusDays(56);
        var incomeList = incomes.findByUserIdAndReceivedOnGreaterThanEqual(userId, windowStart);
        var expenseList = expenses.findByUserIdAndSpentOnGreaterThanEqual(userId, windowStart);

        SafeToSpendResult r = calculator.compute(currency, incomeList, expenseList);
        return new BudgetResponse("dynamic_envelope", r.safeToSpendTodayMinor(), r.currency(),
                r.confidence(), r.envelopes(), r.explanation());
    }
}
