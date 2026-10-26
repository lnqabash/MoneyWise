package app.moneymate.scam.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Request/response payloads for the Scam Scanner APIs (docs/07 §5). */
public final class ScamDtos {

    private ScamDtos() {}

    public record ScanBody(
            @NotBlank String channel,
            @NotBlank String content,
            String language) {}

    public record RedFlag(String code, String explanation) {}

    public record Advice(String summary, List<String> actions) {}

    public record ScanResponse(String reportId, String riskLevel, double riskScore,
                               List<RedFlag> redFlags, Advice advice, String modelVersion) {}

    public record ActionBody(@NotBlank String action) {}

    public record ActionResponse(String status) {}
}
