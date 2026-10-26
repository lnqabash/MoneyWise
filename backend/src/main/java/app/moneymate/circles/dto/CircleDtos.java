package app.moneymate.circles.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Request/response payloads for the Community Circles APIs. */
public final class CircleDtos {

    private CircleDtos() {}

    public record CreateCircleBody(
            @NotBlank String name,
            String type,                 // rosca | asca (default rosca)
            @Min(1) long contributionMinor,
            @NotBlank String currency,
            String cadence,              // weekly | biweekly | monthly
            @Min(2) int memberLimit) {}

    public record JoinBody(@NotNull UUID userId) {}

    public record ContributeBody(@Min(1) long amountMinor) {}

    public record MemberView(String memberId, String userId, int payoutPosition,
                             String status, boolean contributedThisCycle) {}

    public record CycleView(String cycleId, int cycleNumber, String payoutMemberId,
                            long potMinor, String status, int contributionsIn, int totalMembers) {}

    public record CircleView(
            String circleId, String name, String type, long contributionMinor, String currency,
            String cadence, int memberLimit, int memberCount, int currentCycleNo, String status,
            String organiserUserId, List<MemberView> members, CycleView currentCycle) {}

    public record PayoutResponse(String cycleId, String paidToMemberId, long potMinor,
                                 String txnId, int nextCycleNo, String circleStatus) {}
}
