package app.moneymate.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {
    List<Income> findByUserIdAndReceivedOnGreaterThanEqual(UUID userId, LocalDate from);
}
