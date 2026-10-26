package app.moneymate.scam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scam_reports")
public class ScamReport {

    @Id
    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "channel", nullable = false)
    private String channel;

    // We store a salted hash of the content, never the raw message (docs/06 + privacy by design).
    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "risk_score", nullable = false)
    private double riskScore;

    @Lob
    @Column(name = "red_flags")
    private String redFlagsJson;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "user_action")
    private String userAction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScamReport() {}

    public static ScamReport create(UUID userId, String channel, String contentHash,
                                    String riskLevel, double riskScore, String redFlagsJson,
                                    String modelVersion) {
        ScamReport r = new ScamReport();
        r.reportId = UUID.randomUUID();
        r.userId = userId;
        r.channel = channel;
        r.contentHash = contentHash;
        r.riskLevel = riskLevel;
        r.riskScore = riskScore;
        r.redFlagsJson = redFlagsJson;
        r.modelVersion = modelVersion;
        r.createdAt = Instant.now();
        return r;
    }

    public void setUserAction(String action) { this.userAction = action; }

    public UUID getReportId() { return reportId; }
    public UUID getUserId() { return userId; }
}
