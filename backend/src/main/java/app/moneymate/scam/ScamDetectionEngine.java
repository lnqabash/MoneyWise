package app.moneymate.scam;

import app.moneymate.scam.dto.ScamDtos.RedFlag;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Explainable, rule-based scam classifier (on-device / fallback model from
 * docs/08-ai-module-design.md §3). It runs with no external calls so it works
 * offline and is fully transparent — each rule hit IS a red flag with a
 * plain-language reason, which is the user-facing trust mechanism.
 *
 * The cloud model would later add ML scoring + URL reputation + OCR, but the
 * red-flag contract stays identical.
 */
@Component
public class ScamDetectionEngine {

    public static final String MODEL_VERSION = "scam-rules-0.1";

    private record Rule(String code, Pattern pattern, double weight, String explanation) {}

    private static final List<Rule> RULES = List.of(
            new Rule("CREDENTIAL_REQUEST",
                    Pattern.compile("\\b(pin|password|otp|one.?time.?(pin|code|password)|cvv|card number)\\b",
                            Pattern.CASE_INSENSITIVE), 0.45,
                    "Asks for a secret like your PIN or OTP — banks and real services never do this."),
            new Rule("URGENCY",
                    Pattern.compile("\\b(urgent|immediately|act now|within \\d+ ?(min|hour|hrs)|expire|last chance|today only)\\b",
                            Pattern.CASE_INSENSITIVE), 0.2,
                    "Pressures you to act immediately so you don't stop to think."),
            new Rule("PRIZE_WINNINGS",
                    Pattern.compile("\\b(you(?:'ve| have)? won|congratulations|lucky winner|claim your (prize|reward|cash))\\b",
                            Pattern.CASE_INSENSITIVE), 0.25,
                    "Claims you won something you didn't enter — a classic lure."),
            new Rule("UPFRONT_PAYMENT",
                    Pattern.compile("\\b(pay|send|deposit|transfer).{0,20}(fee|to claim|to receive|to unlock|activation)\\b",
                            Pattern.CASE_INSENSITIVE), 0.3,
                    "Asks you to pay money to receive money — legitimate rewards never require this."),
            new Rule("DOUBLE_MONEY",
                    Pattern.compile("\\b(double your money|guaranteed returns?|triple|invest .{0,15}(get|earn) \\d+%?)\\b",
                            Pattern.CASE_INSENSITIVE), 0.35,
                    "Promises guaranteed or doubled money — no real investment can promise this."),
            new Rule("SHORTENED_URL",
                    Pattern.compile("\\b(bit\\.ly|tinyurl|t\\.co|goo\\.gl|b1t\\.ly|is\\.gd|cutt\\.ly)/\\S+",
                            Pattern.CASE_INSENSITIVE), 0.25,
                    "Uses a shortened link that hides where it really takes you."),
            new Rule("SUSPICIOUS_URL",
                    Pattern.compile("https?://(?![\\w.-]*\\.(gov|org)\\b)[\\w.-]*(verify|secure|account|login|claim|update)[\\w.-]*",
                            Pattern.CASE_INSENSITIVE), 0.2,
                    "Links to a look-alike 'verify your account' page used to steal details."),
            new Rule("IMPERSONATION",
                    Pattern.compile("\\b(your bank|sars|tax refund|government grant|srd|account (suspended|locked|blocked))\\b",
                            Pattern.CASE_INSENSITIVE), 0.2,
                    "Pretends to be your bank or a government body to seem trustworthy.")
    );

    public record Verdict(String riskLevel, double riskScore, List<RedFlag> redFlags) {}

    public Verdict classify(String content) {
        List<RedFlag> flags = new ArrayList<>();
        double score = 0.0;
        String text = content == null ? "" : content;

        for (Rule rule : RULES) {
            if (rule.pattern().matcher(text).find()) {
                flags.add(new RedFlag(rule.code(), rule.explanation()));
                score += rule.weight();
            }
        }

        // Combine weights with diminishing returns, capped at 1.0.
        double riskScore = Math.min(1.0, score);
        // A credential request alone is enough to be high risk.
        boolean hardHigh = flags.stream().anyMatch(f -> f.code().equals("CREDENTIAL_REQUEST"))
                && flags.size() >= 2;

        String level;
        if (hardHigh || riskScore >= 0.6) level = "high";
        else if (riskScore >= 0.3) level = "medium";
        else level = "low";

        return new Verdict(level, round2(riskScore), flags);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
