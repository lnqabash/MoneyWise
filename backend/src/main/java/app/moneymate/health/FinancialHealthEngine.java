package app.moneymate.health;

import app.moneymate.budget.Expense;
import app.moneymate.budget.Income;
import app.moneymate.health.dto.HealthDtos.Pillar;
import app.moneymate.savings.SavingsGoal;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * AI Financial Health Score — an inclusion-friendly alternative to a credit score.
 *
 * Rather than judging people on debt and formal-credit history (which excludes
 * the informal economy), it rewards healthy *behaviour* across four pillars,
 * each 0-100, combined into an overall 0-100 score. Every pillar is explainable
 * (see docs/08-ai-module-design.md) so users understand how to improve — the
 * score is a confidence-builder, not a gate.
 *
 * Pillars:
 *   1. Savings Consistency   — how regularly the user saves
 *   2. Financial Resilience  — emergency buffer vs. monthly spend
 *   3. Budget Adherence      — spending within predicted safe-to-spend
 *   4. Income Stability      — how steady income is (lower volatility = higher)
 *
 * This is the transparent, on-device-capable baseline; a trained model can
 * refine the weightings later behind the same contract.
 */
@Component
public class FinancialHealthEngine {

    public static final String MODEL_VERSION = "health-score-0.1";

    // Pillar weights sum to 1.0. Resilience and consistency weighted highest
    // because they matter most for surviving income shocks.
    private static final double W_CONSISTENCY = 0.30;
    private static final double W_RESILIENCE = 0.30;
    private static final double W_ADHERENCE = 0.20;
    private static final double W_STABILITY = 0.20;

    private static final int WINDOW_DAYS = 56; // ~8 weeks

    public record Result(int overallScore, String band, List<Pillar> pillars, String modelVersion) {}

    public Result compute(List<Income> incomes, List<Expense> expenses, List<SavingsGoal> goals) {
        LocalDate windowStart = LocalDate.now().minusDays(WINDOW_DAYS);

        Pillar consistency = savingsConsistency(goals, incomes, windowStart);
        Pillar resilience = financialResilience(expenses, goals, windowStart);
        Pillar adherence = budgetAdherence(incomes, expenses, windowStart);
        Pillar stability = incomeStability(incomes, windowStart);

        double weighted =
                consistency.score() * W_CONSISTENCY +
                resilience.score() * W_RESILIENCE +
                adherence.score() * W_ADHERENCE +
                stability.score() * W_STABILITY;

        int overall = (int) Math.round(weighted);
        return new Result(overall, band(overall),
                List.of(consistency, resilience, adherence, stability), MODEL_VERSION);
    }

    // --- Pillar 1: Savings Consistency ---
    private Pillar savingsConsistency(List<SavingsGoal> goals, List<Income> incomes, LocalDate windowStart) {
        long totalSaved = goals.stream().mapToLong(SavingsGoal::getSavedMinor).sum();
        long totalIncome = incomes.stream()
                .filter(i -> !i.getReceivedOn().isBefore(windowStart))
                .mapToLong(Income::getAmountMinor).sum();
        boolean hasActiveGoal = goals.stream().anyMatch(g -> "active".equals(g.getStatus()) || "reached".equals(g.getStatus()));

        int score;
        String reason;
        if (goals.isEmpty()) {
            score = 20;
            reason = "You haven't set up a savings goal yet — creating one is the quickest way to boost this.";
        } else {
            // Savings rate as a share of income, capped and scaled.
            double rate = totalIncome > 0 ? (double) totalSaved / totalIncome : 0;
            score = clamp((int) Math.round(Math.min(1.0, rate * 4) * 80) + (hasActiveGoal ? 20 : 0));
            reason = "You've saved " + pct(rate) + " of recent income across your goals. Regular small saves raise this.";
        }
        return new Pillar("savings_consistency", "Savings Consistency", score, reason);
    }

    // --- Pillar 2: Financial Resilience (emergency buffer) ---
    private Pillar financialResilience(List<Expense> expenses, List<SavingsGoal> goals, LocalDate windowStart) {
        long monthlyExpense = Math.max(1, expenses.stream()
                .filter(e -> !e.getSpentOn().isBefore(windowStart))
                .mapToLong(Expense::getAmountMinor).sum() / 2); // 8 weeks ~= 2 months
        long buffer = goals.stream()
                .filter(g -> "emergency_fund".equals(g.getGoalType()) || "repair".equals(g.getGoalType()))
                .mapToLong(SavingsGoal::getSavedMinor).sum();
        if (buffer == 0) {
            buffer = goals.stream().mapToLong(SavingsGoal::getSavedMinor).sum() / 2;
        }
        double monthsCovered = (double) buffer / monthlyExpense;
        // 3 months buffer = full marks.
        int score = clamp((int) Math.round(Math.min(1.0, monthsCovered / 3.0) * 100));
        String reason = String.format(
                "Your savings cover about %.1f month(s) of spending. Aim for 3 months to feel secure against shocks.",
                monthsCovered);
        return new Pillar("financial_resilience", "Financial Resilience", score, reason);
    }

    // --- Pillar 3: Budget Adherence ---
    private Pillar budgetAdherence(List<Income> incomes, List<Expense> expenses, LocalDate windowStart) {
        long income = incomes.stream()
                .filter(i -> !i.getReceivedOn().isBefore(windowStart))
                .mapToLong(Income::getAmountMinor).sum();
        long spend = expenses.stream()
                .filter(e -> !e.getSpentOn().isBefore(windowStart))
                .mapToLong(Expense::getAmountMinor).sum();
        if (income == 0) {
            return new Pillar("budget_adherence", "Budget Adherence", 50,
                    "We need a bit more income history to measure how well you stay within budget.");
        }
        double spendRatio = (double) spend / income;
        // Spending <= 80% of income = full marks; scales down as it approaches/exceeds income.
        int score = clamp((int) Math.round((1.0 - Math.min(1.0, Math.max(0, spendRatio - 0.8) / 0.4)) * 100));
        String reason = "You spent " + pct(spendRatio) + " of your recent income. Keeping some back each period lifts this.";
        return new Pillar("budget_adherence", "Budget Adherence", score, reason);
    }

    // --- Pillar 4: Income Stability ---
    private Pillar incomeStability(List<Income> incomes, LocalDate windowStart) {
        // Group income by week and measure volatility across weeks.
        Map<Integer, Long> byWeek = new TreeMap<>();
        for (Income i : incomes) {
            if (i.getReceivedOn().isBefore(windowStart)) continue;
            int week = i.getReceivedOn().getDayOfYear() / 7;
            byWeek.merge(week, i.getAmountMinor(), Long::sum);
        }
        List<Long> weekly = new ArrayList<>(byWeek.values());
        if (weekly.size() < 2) {
            return new Pillar("income_stability", "Income Stability", 50,
                    "Not enough history yet to assess how steady your income is.");
        }
        double cv = coefficientOfVariation(weekly);
        // CV of 0 = perfectly steady = 100; CV >= 1 = very volatile = low.
        int score = clamp((int) Math.round((1.0 - Math.min(1.0, cv)) * 100));
        String reason = cv > 0.5
                ? "Your income is quite uneven week to week — MoneyMate smooths this for you with dynamic budgeting."
                : "Your income has been fairly steady recently, which strengthens your financial footing.";
        return new Pillar("income_stability", "Income Stability", score, reason);
    }

    // --- helpers ---
    private double coefficientOfVariation(List<Long> values) {
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) return 1.0;
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance) / mean;
    }

    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    private String pct(double ratio) { return Math.round(ratio * 100) + "%"; }

    private String band(int score) {
        if (score >= 80) return "Thriving";
        if (score >= 60) return "Building";
        if (score >= 40) return "Finding footing";
        return "Getting started";
    }
}
