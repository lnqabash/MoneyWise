package app.moneymate.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    @Column(name = "challenge_id")
    private UUID challengeId;

    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpChallenge() {}

    public static OtpChallenge create(String phoneE164, String codeHash, long ttlSeconds) {
        OtpChallenge c = new OtpChallenge();
        c.challengeId = UUID.randomUUID();
        c.phoneE164 = phoneE164;
        c.codeHash = codeHash;
        c.createdAt = Instant.now();
        c.expiresAt = c.createdAt.plusSeconds(ttlSeconds);
        return c;
    }

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public void incrementAttempts() { this.attempts++; }
    public void consume() { this.consumed = true; }

    public UUID getChallengeId() { return challengeId; }
    public String getPhoneE164() { return phoneE164; }
    public String getCodeHash() { return codeHash; }
    public int getAttempts() { return attempts; }
    public boolean isConsumed() { return consumed; }
    public Instant getExpiresAt() { return expiresAt; }
}
