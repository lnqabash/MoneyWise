package app.moneymate.circles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "circle_members")
public class CircleMember {

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "circle_id", nullable = false)
    private UUID circleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "payout_position", nullable = false)
    private int payoutPosition;

    @Column(name = "status", nullable = false)
    private String status = "active";

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected CircleMember() {}

    public static CircleMember create(UUID circleId, UUID userId, int payoutPosition) {
        CircleMember m = new CircleMember();
        m.memberId = UUID.randomUUID();
        m.circleId = circleId;
        m.userId = userId;
        m.payoutPosition = payoutPosition;
        m.joinedAt = Instant.now();
        return m;
    }

    public UUID getMemberId() { return memberId; }
    public UUID getCircleId() { return circleId; }
    public UUID getUserId() { return userId; }
    public int getPayoutPosition() { return payoutPosition; }
    public String getStatus() { return status; }
}
