package app.moneymate.savings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {
    List<SavingsGoal> findByUserId(UUID userId);
    Optional<SavingsGoal> findByGoalIdAndUserId(UUID goalId, UUID userId);
}
