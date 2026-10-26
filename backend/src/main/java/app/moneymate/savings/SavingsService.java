package app.moneymate.savings;

import app.moneymate.budget.Expense;
import app.moneymate.budget.ExpenseRepository;
import app.moneymate.common.ApiException;
import app.moneymate.ledger.LedgerService;
import app.moneymate.savings.dto.SavingsDtos.ContributionBody;
import app.moneymate.savings.dto.SavingsDtos.ContributionResponse;
import app.moneymate.savings.dto.SavingsDtos.EmergencyFundResponse;
import app.moneymate.savings.dto.SavingsDtos.GoalBody;
import app.moneymate.savings.dto.SavingsDtos.GoalResponse;
import app.moneymate.savings.dto.SavingsDtos.WithdrawalBody;
import app.moneymate.savings.dto.SavingsDtos.WithdrawalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SavingsService {

    private final SavingsGoalRepository goals;
    private final LedgerService ledger;
    private final ExpenseRepository expenses;

    public SavingsService(SavingsGoalRepository goals, LedgerService ledger,
                          ExpenseRepository expenses) {
        this.goals = goals;
        this.ledger = ledger;
        this.expenses = expenses;
    }

    @Transactional
    public GoalResponse createGoal(UUID userId, GoalBody body) {
        Long ruleAmount = body.rule() == null ? null : body.rule().amountMinor();
        String ruleType = body.rule() == null ? "manual" : body.rule().type();
        SavingsGoal goal = SavingsGoal.create(userId, body.name(), body.goalType(),
                body.targetMinor(), body.currency(), body.targetDate(), ruleType, ruleAmount);
        goals.save(goal);
        return toResponse(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(UUID userId) {
        return goals.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoal(UUID userId, UUID goalId) {
        return toResponse(requireOwned(userId, goalId));
    }

    @Transactional
    public ContributionResponse contribute(UUID userId, UUID goalId, ContributionBody body) {
        SavingsGoal goal = requireOwned(userId, goalId);
        if (!"active".equals(goal.getStatus())) {
            throw ApiException.businessRule("This goal is no longer active");
        }
        goal.contribute(body.amountMinor());
        goals.save(goal);

        // Record the real money movement in the ledger (single source of truth).
        // Idempotency key ties the save to the goal + a timestamp bucket.
        String idem = "save:" + goalId + ":" + System.currentTimeMillis();
        var txn = ledger.recordSaveSweep(userId, body.amountMinor(), goal.getCurrency(), goalId, idem);

        return new ContributionResponse(txn.getTxnId().toString(), goal.getSavedMinor(), goal.getStatus());
    }

    @Transactional
    public WithdrawalResponse withdraw(UUID userId, UUID goalId, WithdrawalBody body) {
        SavingsGoal goal = requireOwned(userId, goalId);
        if (body.amountMinor() > goal.getSavedMinor()) {
            throw ApiException.businessRule("You can't withdraw more than you've saved");
        }
        // Soft-lock friction: protect savings discipline without locking people out.
        if (goal.isSoftLocked() && !body.confirmSoftLock()) {
            throw ApiException.businessRule("Confirm soft-lock override to proceed");
        }

        goal.withdraw(body.amountMinor());
        goals.save(goal);

        String idem = "withdraw:" + goalId + ":" + System.currentTimeMillis();
        var txn = ledger.recordWithdrawal(userId, body.amountMinor(), goal.getCurrency(), goalId, idem);

        long weeklyTarget = computeWeeklyTarget(goal) == null ? 0 : computeWeeklyTarget(goal);
        String warning = weeklyTarget > 0
                ? "This sets your goal back by about "
                    + Math.max(1, Math.round((double) body.amountMinor() / weeklyTarget)) + " week(s)."
                : "This reduces your saved balance.";

        return new WithdrawalResponse(txn.getTxnId().toString(), goal.getSavedMinor(), goal.getStatus(), warning);
    }

    @Transactional(readOnly = true)
    public EmergencyFundResponse emergencyFund(UUID userId) {
        SavingsGoal fund = goals.findByUserId(userId).stream()
                .filter(g -> "emergency_fund".equals(g.getGoalType()))
                .findFirst()
                .orElse(null);

        long saved = fund == null ? 0 : fund.getSavedMinor();
        String currency = fund == null ? "ZAR" : fund.getCurrency();

        LocalDate windowStart = LocalDate.now().minusDays(56);
        long twoMonthSpend = expenses.findByUserIdAndSpentOnGreaterThanEqual(userId, windowStart)
                .stream().mapToLong(Expense::getAmountMinor).sum();
        long monthlyExpenses = Math.max(1, twoMonthSpend / 2);

        double coverage = (double) saved / monthlyExpenses;
        String status = coverage >= 3 ? "secure" : coverage >= 1 ? "building" : "at_risk";
        long targetThreeMonths = monthlyExpenses * 3;
        String recommendation = "Aim for 3 months (" + money(currency, targetThreeMonths) + ") coverage.";

        return new EmergencyFundResponse(
                fund == null ? null : fund.getGoalId().toString(),
                saved, monthlyExpenses, round2(coverage), status, recommendation);
    }

    private SavingsGoal requireOwned(UUID userId, UUID goalId) {
        return goals.findByGoalIdAndUserId(goalId, userId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));
    }

    private GoalResponse toResponse(SavingsGoal g) {
        Long weeklyTarget = computeWeeklyTarget(g);
        return new GoalResponse(
                g.getGoalId().toString(), g.getName(), g.getGoalType(),
                g.getTargetMinor(), g.getSavedMinor(), g.getCurrency(),
                round2(g.progress()), g.getStatus(), g.isSoftLocked(), weeklyTarget);
    }

    /** Breaks the remaining target into an achievable weekly figure (Journey B). */
    private Long computeWeeklyTarget(SavingsGoal g) {
        if (g.getTargetDate() == null) return null;
        long weeks = Math.max(1, ChronoUnit.WEEKS.between(LocalDate.now(), g.getTargetDate()));
        long remaining = Math.max(0, g.getTargetMinor() - g.getSavedMinor());
        return remaining / weeks;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String money(String currency, long minor) {
        String sym = switch (currency) { case "ZAR" -> "R"; case "KES" -> "KSh "; case "USD" -> "$"; default -> currency + " "; };
        return sym + String.format("%.2f", minor / 100.0);
    }
}
