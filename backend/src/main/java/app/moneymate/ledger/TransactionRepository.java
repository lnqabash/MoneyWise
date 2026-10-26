package app.moneymate.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByUserIdOrderByOccurredAtDesc(UUID userId);

    List<Transaction> findByGoalIdOrderByOccurredAtDesc(UUID goalId);
}
