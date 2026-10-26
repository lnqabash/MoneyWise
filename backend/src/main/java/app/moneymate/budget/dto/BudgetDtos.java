package app.moneymate.budget.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Request/response payloads for the budget APIs (docs/07 §3). */
public final class BudgetDtos {

    private BudgetDtos() {}

    public record IncomeBody(
            @Min(0) long amountMinor,
            @NotBlank String currency,
            @NotBlank String source,
            String capture,
            boolean isBusiness,
            @NotNull LocalDate receivedOn) {}

    public record IncomeResponse(String incomeId, boolean reforecastQueued) {}

    public record ExpenseBody(
            @Min(1) long amountMinor,
            @NotBlank String currency,
            String category,          // optional: omit or "auto" to auto-categorise from note
            String note,              // free-text / merchant, used for auto-categorisation
            String capture,
            boolean isBusiness,
            @NotNull LocalDate spentOn) {}

    public record ExpenseResponse(String expenseId, String category, boolean autoCategorised) {}

    public record Envelope(String name, long allocatedMinor, long spentMinor) {}

    public record Explanation(String summary, List<String> reasons, String modelVersion) {}

    public record SafeToSpendResult(long safeToSpendTodayMinor, String currency, double confidence,
                                    List<Envelope> envelopes, Explanation explanation) {}

    public record BudgetResponse(String strategy, long safeToSpendTodayMinor, String currency,
                                 double confidence, List<Envelope> envelopes, Explanation explanation) {}

    // Smart Spending Predictions (docs/07 GET /budget/forecast)
    public record LeanPeriod(String from, String to, long shortfallMinor) {}

    public record ForecastResponse(int horizonDays, long predictedIncomeMinor,
                                   long predictedExpenseMinor, long projectedNetMinor,
                                   String currency, double confidence,
                                   List<LeanPeriod> leanPeriods, Explanation explanation) {}

    // Lump-Sum Smoothing (allocate a windfall across weeks + goals)
    public record SmoothingBody(long amountMinor, String currency, int weeks) {}

    public record WeeklyAllocation(int week, long amountMinor) {}

    public record SmoothingResponse(long amountMinor, String currency, int weeks,
                                    long perWeekMinor, long toEmergencyMinor, long toGoalsMinor,
                                    List<WeeklyAllocation> schedule, Explanation explanation) {}
}
