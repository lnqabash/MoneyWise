package app.moneymate.scam;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScamDetectionEngineTest {

    private final ScamDetectionEngine engine = new ScamDetectionEngine();

    @Test
    void classicPhishingIsHighRiskWithExplanations() {
        var verdict = engine.classify(
                "You won R5000! Click http://b1t.ly/claim to verify your bank PIN immediately");

        assertThat(verdict.riskLevel()).isEqualTo("high");
        assertThat(verdict.riskScore()).isGreaterThanOrEqualTo(0.6);
        assertThat(verdict.redFlags()).extracting("code")
                .contains("CREDENTIAL_REQUEST", "PRIZE_WINNINGS", "SHORTENED_URL", "URGENCY");
    }

    @Test
    void doubleYourMoneyInvestmentScamIsFlagged() {
        var verdict = engine.classify("Join our group and double your money in 7 days, guaranteed returns!");
        assertThat(verdict.riskLevel()).isIn("medium", "high");
        assertThat(verdict.redFlags()).extracting("code").contains("DOUBLE_MONEY");
    }

    @Test
    void benignMessageIsLowRisk() {
        var verdict = engine.classify("Hi, are we still meeting at the market tomorrow at 9?");
        assertThat(verdict.riskLevel()).isEqualTo("low");
        assertThat(verdict.redFlags()).isEmpty();
    }
}
