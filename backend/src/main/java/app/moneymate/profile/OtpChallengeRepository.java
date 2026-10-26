package app.moneymate.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {
}
