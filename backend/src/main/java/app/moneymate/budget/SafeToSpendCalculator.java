package app.moneymate.budget;

import app.moneymate.budget.dto.BudgetDtos.Envelope;
import app.moneymate.budget.dto.BudgetDtos.Explanation;
import app.moneymate.budget.dto.BudgetDtos.SafeToSpendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Explainable, volatility-aware "safe-to-spend today" heuristic.
 *
 * This is the on-device / fallback model from docs/08-ai-module-design.md
 * (§1 Budget): it does not need the LLM or a trained model to run, so it
 * works offline and in the demo. The cloud model would later refine this.
 *
 * Approach (all amounts in minor units):
 *   - avgDailyIncome  = trailing income over the window / window days
 *   - volatilityFactor = scales the daily figure DOWN when income is lumpy,
 *                        so a lucky big day doesn't license overspending
 *   - committedDaily   = recent recurring outflows spread per day
 *   - safeToSpend      = max(0, avgDailyIncome * volatilityFactor - committedDaily)
 *
 * Every number produces a plain-language reason for the explainability layer.
 */
@Component
public class SafeToSpendCalculator {

    private static final int WINDOW_DAYS = 56; // ~8 weeks, matches the docs' "8-week average"

    public SafeToSpendResult compute(String currency, List<Income> incomes, List<Expense> expenses) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(WINDOW_DAYS);

        List<Long> dailyIncomeAmounts = incomes.stream()
                .filter(i -> !i.getReceivedOn().isBefore(windowStart))
                .map(Income::getAmountMinor)
                .toList();

        long totalIncome = dailyIncomeAmounts.stream().mapToLong(Long::longValue).sum();
        long totalExpense = expenses.stream()
                .filter(e -> !e.getSpentOn().isBefore(windowStart))
                .mapToLong(Expense::getAmountMinor).sum();

        long avgDailyIncome = totalIncome / WINDOW_DAYS;
        long committedDaily = totalExpense / WINDOW_DAYS;

        double volatility = coefficientOfVariation(dailyIncomeAmounts);
        // Higher volatility -> more conservative. Clamp factor to [0.6, 1.0].
        double volatilityFactor = Math.max(0.6, 1.0 - Math.min(volatility, 1.0) * 0.4);

        long safeToSpend = Math.max(0,
                Math.round(avgDailyIncome * volatilityFactor) - committedDaily);

        double confidence = dailyIncomeAmounts.isEmpty() ? 0.3
                : Math.max(0.5, 1.0 - Math.min(volatility, 1.0) * 0.5);

        List<String> reasons = buildReasons(
                currency, avgDailyIncome, committedDaily, volatility, volatilityFactor, dailyIncomeAmounts.size());

        List<Envelope> envelopes = deriveEnvelopes(expenses, windowStart);

        Explanation explanation = new Explanation(
                "You can safely spend " + format(currency, safeToSpend) + " today.",
                reasons,
                "budget-heuristic-0.1");

        return new SafeToSpendResult(safeToSpend, currency, round2(confidence), envelopes, explanation);
    }

    private List<String> buildReasons(String currency, long avgDaily, long committedDaily,
                                      double volatility, double factor, int incomeEvents) {
        List<String> reasons = new ArrayList<>();
        if (incomeEvents == 0) {
            reasons.add("We don't have income history yet, so this is a cautious estimate.");
            return reasons;
        }
        reasons.add("Your average daily income over the last 8 weeks is " + format(currency, avgDaily) + ".");
        if (volatility > 0.5) {
            reasons.add("Your income is quite uneven, so we've set a more cautious amount to protect you on slow days.");
        } else {
            reasons.add("Your income has been fairly steady, so we're confident in this amount.");
        }
        if (committedDaily > 0) {
            reasons.add("We set aside " + format(currency, committedDaily) + " a day for your usual bills and spending.");
        }
        return reasons;
    }

    private List<Envelope> deriveEnvelopes(List<Expense> expenses, LocalDate windowStart) {
        // Simple category rollup for the slice; the real service manages persisted envelopes.
        var byCategory = new java.util.LinkedHashMap<String, long[]>();
        for (Expense e : expenses) {
            if (e.getSpentOn().isBefore(windowStart)) continue;
            byCategory.computeIfAbsent(e.getCategory(), k -> new long[1])[0] += e.getAmountMinor();
        }
        List<Envelope> envelopes = new ArrayList<>();
        byCategory.forEach((cat, sum) -> envelopes.add(new Envelope(cat, sum[0], sum[0])));
        return envelopes;
    }

    /** Coefficient of variation = stddev / mean. 0 = perfectly steady income. */
    private double coefficientOfVariation(List<Long> amounts) {
        if (amounts.size() < 2) return 0.0;
        double mean = amounts.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) return 0.0;
        double variance = amounts.stream()
                .mapToDouble(a -> Math.pow(a - mean, 2)).average().orElse(0);
        return Math.sqrt(variance) / mean;
    }

    private String format(String currency, long minor) {
        return currencySymbol(currency) + String.format("%.2f", minor / 100.0);
    }

    private String currencySymbol(String currency) {
        return switch (currency) {
            case "ZAR" -> "R";
            case "KES" -> "KSh ";
            case "USD" -> "$";
            default -> currency + " ";
        };
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
