package app.moneymate.savings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** Request/response payloads for the savings APIs (docs/07 §4). */
public final class SavingsDtos {

    private SavingsDtos() {}

    public record Rule(String type, Long amountMinor) {}

    public record GoalBody(
            @NotBlank String name,
            String goalType,
            @Min(1) long targetMinor,
            @NotBlank String currency,
            LocalDate targetDate,
            Rule rule) {}

    public record GoalResponse(String goalId, String name, String goalType,
                               long targetMinor, long savedMinor, String currency,
                               double progress, String status, boolean softLocked,
                               Long weeklyTargetMinor) {}

    public record ContributionBody(@Min(1) long amountMinor, String source) {}

    public record ContributionResponse(String txnId, long savedMinor, String status) {}
}
