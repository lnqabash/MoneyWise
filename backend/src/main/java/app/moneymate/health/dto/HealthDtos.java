package app.moneymate.health.dto;

import java.util.List;

/** Response payloads for the AI Financial Health Score API. */
public final class HealthDtos {

    private HealthDtos() {}

    public record Pillar(String code, String name, int score, String reason) {}

    public record HealthScoreResponse(
            int overallScore,
            String band,
            List<Pillar> pillars,
            String summary,
            String modelVersion) {}
}
