package app.moneymate.circles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A community savings circle (stokvel / chama / ROSCA). */
@Entity
@Table(name = "circles")
public class Circle {

    @Id
    @Column(name = "circle_id")
    private UUID circleId;

    @Column(name = "organiser_user_id", nullable = false)
    private UUID organiserUserId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type = "rosca";

    @Column(name = "contribution_minor", nullable = false)
    private long contributionMinor;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "cadence", nullable = false)
    private String cadence = "weekly";

    @Column(name = "member_limit", nullable = false)
    private int memberLimit = 12;

    @Column(name = "current_cycle_no", nullable = false)
    private int currentCycleNo = 0;

    @Column(name = "status", nullable = false)
    private String status = "forming";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Circle() {}

    public static Circle create(UUID organiserUserId, String name, String type,
                                long contributionMinor, String currency, String cadence,
                                int memberLimit) {
        Circle c = new Circle();
        c.circleId = UUID.randomUUID();
        c.organiserUserId = organiserUserId;
        c.name = name;
        c.type = type == null ? "rosca" : type;
        c.contributionMinor = contributionMinor;
        c.currency = currency;
        c.cadence = cadence == null ? "weekly" : cadence;
        c.memberLimit = memberLimit <= 0 ? 12 : memberLimit;
        c.createdAt = Instant.now();
        return c;
    }

    public void activate() { this.status = "active"; }
    public void complete() { this.status = "completed"; }
    public void setCurrentCycleNo(int n) { this.currentCycleNo = n; }

    public UUID getCircleId() { return circleId; }
    public UUID getOrganiserUserId() { return organiserUserId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public long getContributionMinor() { return contributionMinor; }
    public String getCurrency() { return currency; }
    public String getCadence() { return cadence; }
    public int getMemberLimit() { return memberLimit; }
    public int getCurrentCycleNo() { return currentCycleNo; }
    public String getStatus() { return status; }
}
