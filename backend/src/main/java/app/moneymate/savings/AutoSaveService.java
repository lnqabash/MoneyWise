package app.moneymate.savings;

import app.moneymate.budget.Income;
import app.moneymate.budget.IncomeRepository;
import app.moneymate.ledger.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Executes automatic savings rules — the "Micro-Savings Engine" from the
 * design. Crucially it is AFFORDABILITY-AWARE (Journey B): on a low-income
 * week it reduces or skips the sweep so a vulnerable user is never overdrawn.
 *
 * Rules supported:
 *   - fixed_weekly : sweep the fixed rule amount every run
 *   - good_day     : only sweep when recent income is above the user's average
 *   - roundup      : (simplified) sweep a small fixed nudge each run
 *
 * Driven by AutoSaveScheduler; also callable on demand (useful for the demo).
 */
@Service
public class AutoSaveService {

    private static final Logger log = LoggerFactory.getLogger(AutoSaveService.class);
    private static final int WINDOW_DAYS = 56;

    private final SavingsGoalRepository goals;
    private final IncomeRepository incomes;
    private final LedgerService ledger;

    public AutoSaveService(SavingsGoalRepository goals, IncomeRepository incomes, LedgerService ledger) {
        this.goals = goals;
        this.incomes = incomes;
        this.ledger = ledger;
    }

    /** Runs auto-save for every active goal that has an automatic rule. */
    @Transactional
    public int runAll() {
        List<SavingsGoal> candidates = goals.findByStatusAndRuleTypeNot("active", "manual");
        int swept = 0;
        for (SavingsGoal goal : candidates) {
            if (runFor(goal)) swept++;
        }
        log.info("Auto-save run complete: {} of {} goals swept", swept, candidates.size());
        return swept;
    }

    /** Returns true if a sweep happened for this goal. */
    @Transactional
    public boolean runFor(SavingsGoal goal) {
        long base = goal.getRuleAmountMinor() == null ? 0 : goal.getRuleAmountMinor();
        if (base <= 0 && !"roundup".equals(goal.getRuleType())) return false;

        long amount = switch (goal.getRuleType()) {
            case "fixed_weekly" -> affordabilityAdjusted(goal.getUserId(), base);
            case "good_day" -> isGoodWeek(goal.getUserId()) ? base : 0;
            case "roundup" -> 500; // simplified R5 nudge
            default -> 0;
        };

        if (amount <= 0) {
            log.debug("Skipping goal {} this run (rule={}, not affordable/applicable)",
                    goal.getGoalId(), goal.getRuleType());
            return false;
        }

        goal.contribute(amount);
        goals.save(goal);
        String idem = "autosave:" + goal.getGoalId() + ":" + LocalDate.now();
        ledger.recordSaveSweep(goal.getUserId(), amount, goal.getCurrency(), goal.getGoalId(), idem);
        return true;
    }

    /**
     * Reduce the sweep on lean weeks: if recent income is well below the user's
     * average, halve (or skip) the amount so we never push them into the red.
     */
    private long affordabilityAdjusted(UUID userId, long base) {
        double ratio = recentVsAverageRatio(userId);
        if (ratio < 0.4) return 0;           // very lean week -> skip
        if (ratio < 0.8) return base / 2;    // below average -> reduce
        return base;                          // comfortable -> full amount
    }

    private boolean isGoodWeek(UUID userId) {
        return recentVsAverageRatio(userId) >= 1.0;
    }

    /** Ratio of the last 7 days' income to the weekly average over the window. */
    private double recentVsAverageRatio(UUID userId) {
        LocalDate today = LocalDate.now();
        var window = incomes.findByUserIdAndReceivedOnGreaterThanEqual(userId, today.minusDays(WINDOW_DAYS));
        if (window.isEmpty()) return 0.0;

        long total = window.stream().mapToLong(Income::getAmountMinor).sum();
        double weeklyAvg = (double) total / (WINDOW_DAYS / 7.0);
        if (weeklyAvg == 0) return 0.0;

        long last7 = window.stream()
                .filter(i -> !i.getReceivedOn().isBefore(today.minusDays(7)))
                .mapToLong(Income::getAmountMinor).sum();
        return last7 / weeklyAvg;
    }
}
