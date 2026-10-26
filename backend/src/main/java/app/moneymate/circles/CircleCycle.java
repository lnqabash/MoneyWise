package app.moneymate.circles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One cycle of a circle; in a ROSCA the pot pays out to one member. */
@Entity
@Table(name = "circle_cycles")
public class CircleCycle {

    @Id
    @Column(name = "cycle_id")
    private UUID cycleId;

    @Column(name = "circle_id", nullable = false)
    private UUID circleId;

    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @Column(name = "payout_member_id")
    private UUID payoutMemberId;

    @Column(name = "pot_minor", nullable = false)
    private long potMinor = 0;

    @Column(name = "status", nullable = false)
    private String status = "open";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CircleCycle() {}

    public static CircleCycle create(UUID circleId, int cycleNumber, UUID payoutMemberId) {
        CircleCycle c = new CircleCycle();
        c.cycleId = UUID.randomUUID();
        c.circleId = circleId;
        c.cycleNumber = cycleNumber;
        c.payoutMemberId = payoutMemberId;
        c.createdAt = Instant.now();
        return c;
    }

    public void addToPot(long amountMinor) { this.potMinor += amountMinor; }
    public void markPaid() { this.status = "paid"; }
    public void close() { this.status = "closed"; }

    public UUID getCycleId() { return cycleId; }
    public UUID getCircleId() { return circleId; }
    public int getCycleNumber() { return cycleNumber; }
    public UUID getPayoutMemberId() { return payoutMemberId; }
    public long getPotMinor() { return potMinor; }
    public String getStatus() { return status; }
}
