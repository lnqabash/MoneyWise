package app.moneymate.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "phone_e164", nullable = false, unique = true)
    private String phoneE164;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "preferred_language", nullable = false)
    private String preferredLanguage = "en";

    @Column(name = "preferred_channel", nullable = false)
    private String preferredChannel = "app";

    @Column(name = "persona_segment")
    private String personaSegment;

    @Column(name = "income_pattern")
    private String incomePattern;

    @Column(name = "currency", nullable = false)
    private String currency = "ZAR";

    @Column(name = "kyc_level", nullable = false)
    private String kycLevel = "lite";

    @Column(name = "consent_data_ai", nullable = false)
    private boolean consentDataAi = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    public static User create(String phoneE164, String currency) {
        User u = new User();
        u.userId = UUID.randomUUID();
        u.phoneE164 = phoneE164;
        u.currency = currency == null ? "ZAR" : currency;
        u.createdAt = Instant.now();
        u.updatedAt = u.createdAt;
        return u;
    }

    public void touch() { this.updatedAt = Instant.now(); }

    public UUID getUserId() { return userId; }
    public String getPhoneE164() { return phoneE164; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String v) { this.preferredLanguage = v; }
    public String getPreferredChannel() { return preferredChannel; }
    public void setPreferredChannel(String v) { this.preferredChannel = v; }
    public String getPersonaSegment() { return personaSegment; }
    public void setPersonaSegment(String v) { this.personaSegment = v; }
    public String getIncomePattern() { return incomePattern; }
    public void setIncomePattern(String v) { this.incomePattern = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getKycLevel() { return kycLevel; }
    public boolean isConsentDataAi() { return consentDataAi; }
    public void setConsentDataAi(boolean v) { this.consentDataAi = v; }
    public boolean isActive() { return active; }
}
