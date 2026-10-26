package app.moneymate.profile;

import app.moneymate.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues, rotates, and revokes refresh tokens. The raw token is returned to
 * the client once; only its SHA-256 hash is persisted. Refreshing rotates the
 * token (old one revoked) — reuse of a rotated/revoked token is rejected,
 * which is the standard refresh-token-reuse defence (docs/09).
 */
@Service
public class RefreshTokenService {

    private final SecureRandom random = new SecureRandom();
    private final RefreshTokenRepository repo;
    private final long ttlDays;

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${moneymate.security.refresh-token-ttl-days:30}") long ttlDays) {
        this.repo = repo;
        this.ttlDays = ttlDays;
    }

    /** Creates a new refresh token for the user and returns the RAW value. */
    @Transactional
    public String issue(UUID userId) {
        String raw = randomToken();
        repo.save(RefreshToken.create(userId, hash(raw), ttlDays));
        return raw;
    }

    /**
     * Validates a raw refresh token, rotates it (revoke old, issue new), and
     * returns the owning user id plus the new raw token.
     */
    @Transactional
    public Rotated rotate(String rawToken) {
        RefreshToken existing = repo.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthenticated("Invalid refresh token"));
        if (!existing.isActive()) {
            throw ApiException.unauthenticated("Refresh token expired or revoked");
        }
        existing.revoke();
        repo.save(existing);
        String newRaw = issue(existing.getUserId());
        return new Rotated(existing.getUserId(), newRaw);
    }

    @Transactional
    public void revoke(String rawToken) {
        repo.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            t.revoke();
            repo.save(t);
        });
    }

    public record Rotated(UUID userId, String rawToken) {}

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
