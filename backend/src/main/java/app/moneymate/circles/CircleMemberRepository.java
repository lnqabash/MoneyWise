package app.moneymate.circles;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CircleMemberRepository extends JpaRepository<CircleMember, UUID> {
    List<CircleMember> findByCircleIdOrderByPayoutPositionAsc(UUID circleId);
    List<CircleMember> findByUserId(UUID userId);
    Optional<CircleMember> findByCircleIdAndUserId(UUID circleId, UUID userId);
    long countByCircleId(UUID circleId);
    boolean existsByCircleIdAndUserId(UUID circleId, UUID userId);
}
