package app.moneymate.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneE164(String phoneE164);
    boolean existsByPhoneE164(String phoneE164);
}
