package app.moneymate.health;

import app.moneymate.budget.ExpenseRepository;
import app.moneymate.budget.IncomeRepository;
import app.moneymate.health.dto.HealthDtos.HealthScoreResponse;
import app.moneymate.savings.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class HealthService {

    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;
    private final SavingsGoalRepository goals;
    private final FinancialHealthEngine engine;

    public HealthService(IncomeRepository incomes, ExpenseRepository expenses,
                         SavingsGoalRepository goals, FinancialHealthEngine engine) {
        this.incomes = incomes;
        this.expenses = expenses;
        this.goals = goals;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public HealthScoreResponse score(UUID userId) {
        LocalDate windowStart = LocalDate.now().minusDays(56);
        var incomeList = incomes.findByUserIdAndReceivedOnGreaterThanEqual(userId, windowStart);
        var expenseList = expenses.findByUserIdAndSpentOnGreaterThanEqual(userId, windowStart);
        var goalList = goals.findByUserId(userId);

        var result = engine.compute(incomeList, expenseList, goalList);
        String summary = buildSummary(result.overallScore(), result.band());
        return new HealthScoreResponse(
                result.overallScore(), result.band(), result.pillars(), summary, result.modelVersion());
    }

    private String buildSummary(int score, String band) {
        return switch (band) {
            case "Thriving" -> "You're thriving — your money habits are strong. Keep it up!";
            case "Building" -> "You're building real financial strength. A few small steps will push you higher.";
            case "Finding footing" -> "You're finding your footing. Saving a little, regularly, is the fastest win.";
            default -> "Everyone starts somewhere. Set one small goal today and your score will start climbing.";
        };
    }
}
