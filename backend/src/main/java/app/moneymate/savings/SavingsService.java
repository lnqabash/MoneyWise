package app.moneymate.savings;

import app.moneymate.common.ApiException;
import app.moneymate.savings.dto.SavingsDtos.ContributionBody;
import app.moneymate.savings.dto.SavingsDtos.ContributionResponse;
import app.moneymate.savings.dto.SavingsDtos.GoalBody;
import app.moneymate.savings.dto.SavingsDtos.GoalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SavingsService {

    private final SavingsGoalRepository goals;

    public SavingsService(SavingsGoalRepository goals) {
        this.goals = goals;
    }

    @Transactional
    public GoalResponse createGoal(UUID userId, GoalBody body) {
        Long ruleAmount = body.rule() == null ? null : body.rule().amountMinor();
        String ruleType = body.rule() == null ? "manual" : body.rule().type();
        SavingsGoal goal = SavingsGoal.create(userId, body.name(), body.goalType(),
                body.targetMinor(), body.currency(), body.targetDate(), ruleType, ruleAmount);
        goals.save(goal);
        return toResponse(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(UUID userId) {
        return goals.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoal(UUID userId, UUID goalId) {
        return toResponse(requireOwned(userId, goalId));
    }

    @Transactional
    public ContributionResponse contribute(UUID userId, UUID goalId, ContributionBody body) {
        SavingsGoal goal = requireOwned(userId, goalId);
        if (!"active".equals(goal.getStatus())) {
            throw ApiException.businessRule("This goal is no longer active");
        }
        goal.contribute(body.amountMinor());
        goals.save(goal);
        // Real impl: write a transactions ledger row + publish savings.swept event.
        String txnId = UUID.randomUUID().toString();
        return new ContributionResponse(txnId, goal.getSavedMinor(), goal.getStatus());
    }

    private SavingsGoal requireOwned(UUID userId, UUID goalId) {
        return goals.findByGoalIdAndUserId(goalId, userId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));
    }

    private GoalResponse toResponse(SavingsGoal g) {
        Long weeklyTarget = computeWeeklyTarget(g);
        return new GoalResponse(
                g.getGoalId().toString(), g.getName(), g.getGoalType(),
                g.getTargetMinor(), g.getSavedMinor(), g.getCurrency(),
                round2(g.progress()), g.getStatus(), g.isSoftLocked(), weeklyTarget);
    }

    /** Breaks the remaining target into an achievable weekly figure (Journey B). */
    private Long computeWeeklyTarget(SavingsGoal g) {
        if (g.getTargetDate() == null) return null;
        long weeks = Math.max(1, ChronoUnit.WEEKS.between(LocalDate.now(), g.getTargetDate()));
        long remaining = Math.max(0, g.getTargetMinor() - g.getSavedMinor());
        return remaining / weeks;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
