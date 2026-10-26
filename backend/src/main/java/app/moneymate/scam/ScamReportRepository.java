package app.moneymate.scam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScamReportRepository extends JpaRepository<ScamReport, UUID> {
    Optional<ScamReport> findByReportIdAndUserId(UUID reportId, UUID userId);
}
