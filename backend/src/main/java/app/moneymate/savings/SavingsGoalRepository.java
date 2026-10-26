package app.moneymate.savings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {
    List<SavingsGoal> findByUserId(UUID userId);
    Optional<SavingsGoal> findByGoalIdAndUserId(UUID goalId, UUID userId);

    // Active goals with an automatic rule (not manual), for the auto-save runner.
    List<SavingsGoal> findByStatusAndRuleTypeNot(String status, String ruleType);
}
