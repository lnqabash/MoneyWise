package app.moneymate.credit;

import app.moneymate.health.HealthService;
import app.moneymate.health.dto.HealthDtos.HealthScoreResponse;
import app.moneymate.ledger.LedgerService;
import app.moneymate.ledger.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Alternative credit-readiness scoring (Phase 3, Feature #18).
 *
 * The financially excluded have no bureau history, so this derives a
 * consent-based readiness indicator from in-app BEHAVIOUR: the Financial
 * Health Score plus ledger activity (consistency of money movement). It is
 * explicitly NOT a credit decision — it's a readiness signal plus guidance,
 * fully explainable, that a lending partner could later consume with consent.
 */
@Service
public class CreditReadinessService {

    private final HealthService healthService;
    private final LedgerService ledger;

    public CreditReadinessService(HealthService healthService, LedgerService ledger) {
        this.healthService = healthService;
        this.ledger = ledger;
    }

    public record ReadinessResponse(int readinessScore, String tier, List<String> strengths,
                                    List<String> improvements, String disclaimer, String modelVersion) {}

    @Transactional(readOnly = true)
    public ReadinessResponse assess(UUID userId) {
        HealthScoreResponse health = healthService.score(userId);
        List<Transaction> txns = ledger.historyForUser(userId);

        // Activity signal: more consistent, varied money movement -> more readiness.
        long activityCount = txns.size();
        int activityScore = (int) Math.min(100, activityCount * 8); // ~12+ txns = full marks

        // Blend health (70%) with activity (30%).
        int readiness = (int) Math.round(health.overallScore() * 0.7 + activityScore * 0.3);

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        health.pillars().forEach(p -> {
            if (p.score() >= 70) strengths.add(p.name() + " is strong (" + p.score() + "/100).");
            else improvements.add("Raise your " + p.name() + ": " + p.reason());
        });
        if (activityScore >= 60) strengths.add("You have a consistent record of money activity in the app.");
        else improvements.add("Keep recording income and saving regularly to build a track record.");

        return new ReadinessResponse(
                readiness, tier(readiness), strengths, improvements,
                "This is a financial-readiness guide based on your own activity, not a credit decision "
                        + "or guarantee of a loan. Sharing it with a lender is always your choice.",
                "credit-readiness-0.1");
    }

    private String tier(int score) {
        if (score >= 80) return "Ready";
        if (score >= 60) return "Nearly ready";
        if (score >= 40) return "Building readiness";
        return "Getting started";
    }
}
