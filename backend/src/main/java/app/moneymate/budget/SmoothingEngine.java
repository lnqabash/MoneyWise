package app.moneymate.budget;

import app.moneymate.budget.dto.BudgetDtos.Explanation;
import app.moneymate.budget.dto.BudgetDtos.SmoothingResponse;
import app.moneymate.budget.dto.BudgetDtos.WeeklyAllocation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lump-Sum Smoothing ("personal salary") — turns a one-off windfall into a
 * steady weekly draw, with slices set aside for an emergency buffer and goals
 * (Feature #15, for feast-or-famine earners like the freelancer persona).
 *
 * Allocation policy (explainable, not a black box):
 *   10% -> emergency buffer, 15% -> goals, remainder -> smoothed weekly salary.
 */
@Component
public class SmoothingEngine {

    private static final double EMERGENCY_SHARE = 0.10;
    private static final double GOALS_SHARE = 0.15;

    public SmoothingResponse smooth(String currency, long amountMinor, int weeks) {
        int w = Math.max(1, weeks);
        long toEmergency = Math.round(amountMinor * EMERGENCY_SHARE);
        long toGoals = Math.round(amountMinor * GOALS_SHARE);
        long salaryPool = amountMinor - toEmergency - toGoals;
        long perWeek = salaryPool / w;

        List<WeeklyAllocation> schedule = new ArrayList<>();
        long distributed = 0;
        for (int i = 1; i <= w; i++) {
            // Put any rounding remainder into the final week.
            long amt = (i == w) ? (salaryPool - distributed) : perWeek;
            schedule.add(new WeeklyAllocation(i, amt));
            distributed += amt;
        }

        List<String> reasons = List.of(
                "We set aside " + money(currency, toEmergency) + " for emergencies and "
                        + money(currency, toGoals) + " towards your goals.",
                "The rest becomes a steady " + money(currency, perWeek)
                        + " a week for " + w + " weeks — your own personal salary.",
                "This smooths a lumpy payment into predictable income so lean weeks don't hurt.");
        Explanation explanation = new Explanation(
                "Your " + money(currency, amountMinor) + " is now a steady weekly income.",
                reasons, "smoothing-0.1");

        return new SmoothingResponse(amountMinor, currency, w, perWeek, toEmergency, toGoals,
                schedule, explanation);
    }

    private String money(String currency, long minor) {
        String sym = switch (currency) { case "ZAR" -> "R"; case "KES" -> "KSh "; case "USD" -> "$"; default -> currency + " "; };
        return sym + String.format("%.2f", minor / 100.0);
    }
}
