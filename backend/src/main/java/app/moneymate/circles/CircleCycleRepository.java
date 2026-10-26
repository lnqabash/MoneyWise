package app.moneymate.circles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CircleCycleRepository extends JpaRepository<CircleCycle, UUID> {
    List<CircleCycle> findByCircleIdOrderByCycleNumberAsc(UUID circleId);
    Optional<CircleCycle> findByCircleIdAndCycleNumber(UUID circleId, int cycleNumber);
}
