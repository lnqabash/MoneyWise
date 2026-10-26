package app.moneymate.circles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "circle_contributions")
public class CircleContribution {

    @Id
    @Column(name = "contribution_id")
    private UUID contributionId;

    @Column(name = "cycle_id", nullable = false)
    private UUID cycleId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "status", nullable = false)
    private String status = "paid";

    @Column(name = "paid_at")
    private Instant paidAt;

    protected CircleContribution() {}

    public static CircleContribution createPaid(UUID cycleId, UUID memberId, long amountMinor) {
        CircleContribution c = new CircleContribution();
        c.contributionId = UUID.randomUUID();
        c.cycleId = cycleId;
        c.memberId = memberId;
        c.amountMinor = amountMinor;
        c.status = "paid";
        c.paidAt = Instant.now();
        return c;
    }

    public UUID getContributionId() { return contributionId; }
    public UUID getCycleId() { return cycleId; }
    public UUID getMemberId() { return memberId; }
    public long getAmountMinor() { return amountMinor; }
    public String getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
}
