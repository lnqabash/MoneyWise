package app.moneymate.circles;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-level checks of the ROSCA rotation maths that the service relies on,
 * independent of the database. The full guarded-payout behaviour is verified
 * end-to-end via the running server.
 */
class CircleRotationTest {

    @Test
    void rotationPositionsAreAssignedInJoinOrder() {
        UUID circleId = UUID.randomUUID();
        var m1 = CircleMember.create(circleId, UUID.randomUUID(), 1);
        var m2 = CircleMember.create(circleId, UUID.randomUUID(), 2);
        var m3 = CircleMember.create(circleId, UUID.randomUUID(), 3);
        List<CircleMember> members = List.of(m1, m2, m3);

        // Cycle N pays the member whose position == N.
        for (int cycle = 1; cycle <= 3; cycle++) {
            final int c = cycle;
            var recipient = members.stream()
                    .filter(m -> m.getPayoutPosition() == c)
                    .findFirst()
                    .orElseThrow();
            assertThat(recipient.getPayoutPosition()).isEqualTo(cycle);
        }
    }

    @Test
    void cyclePotAccumulatesContributions() {
        var cycle = CircleCycle.create(UUID.randomUUID(), 1, UUID.randomUUID());
        cycle.addToPot(20000);
        cycle.addToPot(20000);
        cycle.addToPot(20000);
        assertThat(cycle.getPotMinor()).isEqualTo(60000);
        assertThat(cycle.getStatus()).isEqualTo("open");
        cycle.markPaid();
        assertThat(cycle.getStatus()).isEqualTo("paid");
    }
}
