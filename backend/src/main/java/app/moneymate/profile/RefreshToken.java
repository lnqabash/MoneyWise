package app.moneymate.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A rotating, revocable refresh token. Only the hash of the raw value is stored. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "token_id")
    private UUID tokenId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    public static RefreshToken create(UUID userId, String tokenHash, long ttlDays) {
        RefreshToken t = new RefreshToken();
        t.tokenId = UUID.randomUUID();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.createdAt = Instant.now();
        t.expiresAt = t.createdAt.plusSeconds(ttlDays * 86400);
        return t;
    }

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isActive() { return !revoked && !isExpired(); }
    public void revoke() { this.revoked = true; }

    public UUID getTokenId() { return tokenId; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
}
