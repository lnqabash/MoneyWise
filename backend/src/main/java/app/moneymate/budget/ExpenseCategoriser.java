package app.moneymate.budget;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Rule-based expense auto-categorisation (Feature #13, v1). Infers a category
 * from a free-text note/merchant when the client doesn't supply one. This is
 * the transparent baseline; a trained text classifier replaces the rules later
 * behind the same method signature.
 */
@Component
public class ExpenseCategoriser {

    private record Rule(String category, Pattern pattern) {}

    // Order matters: first match wins. Keywords are locale-aware examples.
    private static final List<Rule> RULES = List.of(
            rule("fuel", "petrol|diesel|fuel|garage|engen|shell|bp|total|caltex"),
            rule("transport", "taxi|bus|uber|bolt|train|fare|transport|toll"),
            rule("food", "food|grocer|groceries|spar|shoprite|pick n pay|checkers|woolworths|market|bread|milk|maize|meal"),
            rule("airtime", "airtime|data|mtn|vodacom|cell c|telkom|bundle"),
            rule("utilities", "electricity|prepaid|water|rent|rates|municipal"),
            rule("health", "clinic|pharmacy|chemist|doctor|medicine|hospital"),
            rule("school", "school|fees|tuition|uniform|stationery|books"),
            rule("stock", "stock|wholesale|supplier|inventory|goods"),
            rule("airtime", "whatsapp|facebook")
    );

    /**
     * Returns the category. If the caller passed a concrete category (not null
     * and not "auto"), it is kept. Otherwise we infer from the note.
     */
    public Result categorise(String providedCategory, String note) {
        if (providedCategory != null && !providedCategory.isBlank()
                && !providedCategory.equalsIgnoreCase("auto")) {
            return new Result(providedCategory, false);
        }
        String text = note == null ? "" : note.toLowerCase();
        for (Rule r : RULES) {
            if (r.pattern().matcher(text).find()) {
                return new Result(r.category(), true);
            }
        }
        return new Result("other", true);
    }

    public record Result(String category, boolean autoCategorised) {}

    private static Rule rule(String category, String regex) {
        return new Rule(category, Pattern.compile("\\b(" + regex + ")\\b", Pattern.CASE_INSENSITIVE));
    }
}
