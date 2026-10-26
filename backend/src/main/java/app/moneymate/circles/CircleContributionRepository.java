package app.moneymate.circles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CircleContributionRepository extends JpaRepository<CircleContribution, UUID> {
    List<CircleContribution> findByCycleId(UUID cycleId);
    Optional<CircleContribution> findByCycleIdAndMemberId(UUID cycleId, UUID memberId);
    long countByCycleId(UUID cycleId);
}
