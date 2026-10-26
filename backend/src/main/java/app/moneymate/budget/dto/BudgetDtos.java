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
            @NotBlank String category,
            String capture,
            boolean isBusiness,
            @NotNull LocalDate spentOn) {}

    public record ExpenseResponse(String expenseId, String category) {}

    public record Envelope(String name, long allocatedMinor, long spentMinor) {}

    public record Explanation(String summary, List<String> reasons, String modelVersion) {}

    public record SafeToSpendResult(long safeToSpendTodayMinor, String currency, double confidence,
                                    List<Envelope> envelopes, Explanation explanation) {}

    public record BudgetResponse(String strategy, long safeToSpendTodayMinor, String currency,
                                 double confidence, List<Envelope> envelopes, Explanation explanation) {}
}
