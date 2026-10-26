package app.moneymate.budget;

import app.moneymate.budget.dto.BudgetDtos.Explanation;
import app.moneymate.budget.dto.BudgetDtos.ForecastResponse;
import app.moneymate.budget.dto.BudgetDtos.LeanPeriod;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Smart Spending Predictions — projects income/expense over a horizon and
 * flags "lean weeks" where predicted outflow exceeds predicted inflow
 * (Journey A / Feature #2). This is the explainable, on-device-capable
 * baseline (no ML service needed); a trained seasonality model refines it later.
 *
 * Method: use the trailing 8-week daily averages as the forward run-rate,
 * then walk the horizon week by week to find shortfalls.
 */
@Component
public class ForecastEngine {

    private static final int WINDOW_DAYS = 56;

    public ForecastResponse forecast(String currency, List<Income> incomes,
                                     List<Expense> expenses, int horizonDays) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(WINDOW_DAYS);

        long windowIncome = incomes.stream()
                .filter(i -> !i.getReceivedOn().isBefore(windowStart))
                .mapToLong(Income::getAmountMinor).sum();
        long windowExpense = expenses.stream()
                .filter(e -> !e.getSpentOn().isBefore(windowStart))
                .mapToLong(Expense::getAmountMinor).sum();

        double dailyIncome = (double) windowIncome / WINDOW_DAYS;
        double dailyExpense = (double) windowExpense / WINDOW_DAYS;

        long predictedIncome = Math.round(dailyIncome * horizonDays);
        long predictedExpense = Math.round(dailyExpense * horizonDays);
        long projectedNet = predictedIncome - predictedExpense;

        // Walk week by week; flag weeks where expense run-rate exceeds income.
        List<LeanPeriod> lean = new ArrayList<>();
        long weeklyIncome = Math.round(dailyIncome * 7);
        long weeklyExpense = Math.round(dailyExpense * 7);
        if (weeklyExpense > weeklyIncome) {
            int weeks = Math.max(1, horizonDays / 7);
            for (int w = 0; w < weeks; w++) {
                LocalDate from = today.plusDays((long) w * 7);
                LocalDate to = from.plusDays(6);
                lean.add(new LeanPeriod(from.toString(), to.toString(),
                        weeklyExpense - weeklyIncome));
            }
        }

        // Confidence falls with a shorter history and a longer horizon.
        int incomeEvents = (int) incomes.stream()
                .filter(i -> !i.getReceivedOn().isBefore(windowStart)).count();
        double confidence = incomeEvents == 0 ? 0.3
                : Math.max(0.4, Math.min(0.9, 0.9 - (horizonDays / 60.0) * 0.3));

        Explanation explanation = buildExplanation(currency, predictedIncome, predictedExpense, lean);

        return new ForecastResponse(horizonDays, predictedIncome, predictedExpense, projectedNet,
                currency, round2(confidence), lean, explanation);
    }

    private Explanation buildExplanation(String currency, long income, long expense, List<LeanPeriod> lean) {
        List<String> reasons = new ArrayList<>();
        reasons.add("Based on your last 8 weeks, we expect about " + money(currency, income)
                + " in and " + money(currency, expense) + " out over this period.");
        if (!lean.isEmpty()) {
            reasons.add("Heads up: spending looks likely to outpace income in "
                    + lean.size() + " week(s). Setting a little aside now will help.");
        } else {
            reasons.add("You're on track to stay in the positive over this period.");
        }
        String summary = lean.isEmpty()
                ? "You look set to cover your spending."
                : "A lean stretch may be coming — a small buffer now will cushion it.";
        return new Explanation(summary, reasons, "forecast-heuristic-0.1");
    }

    private String money(String currency, long minor) {
        String sym = switch (currency) { case "ZAR" -> "R"; case "KES" -> "KSh "; case "USD" -> "$"; default -> currency + " "; };
        return sym + String.format("%.2f", minor / 100.0);
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
