package app.moneymate.circles;

import app.moneymate.circles.dto.CircleDtos.*;
import app.moneymate.common.ApiException;
import app.moneymate.ledger.LedgerService;
import app.moneymate.profile.User;
import app.moneymate.profile.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Community Savings Circles (ROSCA) business logic.
 *
 * Trust/safety design (docs/16 §3):
 *  - Transparent ledger: anyone in the circle can see all contributions & payouts.
 *  - Fair rotation: payout order is the join order (visible), not the organiser's choice.
 *  - Guarded payout: the pot can only be released once EVERY active member has
 *    contributed for the cycle; it always pays the scheduled rotation member, so
 *    no single party (including the organiser) can divert funds.
 *  - Every money movement posts to the central transactions ledger.
 */
@Service
public class CircleService {

    private final CircleRepository circles;
    private final CircleMemberRepository members;
    private final CircleCycleRepository cycles;
    private final CircleContributionRepository contributions;
    private final UserRepository users;
    private final LedgerService ledger;

    public CircleService(CircleRepository circles, CircleMemberRepository members,
                         CircleCycleRepository cycles, CircleContributionRepository contributions,
                         UserRepository users, LedgerService ledger) {
        this.circles = circles;
        this.members = members;
        this.cycles = cycles;
        this.contributions = contributions;
        this.users = users;
        this.ledger = ledger;
    }

    @Transactional
    public CircleView create(UUID organiserId, CreateCircleBody body) {
        Circle circle = Circle.create(organiserId, body.name(), body.type(),
                body.contributionMinor(), body.currency(), body.cadence(), body.memberLimit());
        circles.save(circle);
        // Organiser is member #1.
        members.save(CircleMember.create(circle.getCircleId(), organiserId, 1));
        return view(circle);
    }

    @Transactional
    public CircleView join(UUID circleId, UUID newUserId) {
        Circle circle = requireCircle(circleId);
        if (!"forming".equals(circle.getStatus())) {
            throw ApiException.businessRule("This circle is no longer accepting members");
        }
        if (members.existsByCircleIdAndUserId(circleId, newUserId)) {
            throw ApiException.conflict("Already a member of this circle");
        }
        long count = members.countByCircleId(circleId);
        if (count >= circle.getMemberLimit()) {
            throw ApiException.businessRule("This circle is full");
        }
        int position = (int) count + 1;
        members.save(CircleMember.create(circleId, newUserId, position));
        return view(circle);
    }

    /** Organiser-only: locks membership and opens cycle 1. */
    @Transactional
    public CircleView start(UUID callerId, UUID circleId) {
        Circle circle = requireCircle(circleId);
        requireOrganiser(circle, callerId);
        if (!"forming".equals(circle.getStatus())) {
            throw ApiException.businessRule("Circle already started");
        }
        if (members.countByCircleId(circleId) < 2) {
            throw ApiException.businessRule("A circle needs at least 2 members to start");
        }
        circle.activate();
        circle.setCurrentCycleNo(1);
        circles.save(circle);
        openCycle(circle, 1);
        return view(circle);
    }

    @Transactional
    public CycleView contribute(UUID callerId, UUID circleId, ContributeBody body) {
        Circle circle = requireCircle(circleId);
        if (!"active".equals(circle.getStatus())) {
            throw ApiException.businessRule("Circle is not active");
        }
        CircleMember member = members.findByCircleIdAndUserId(circleId, callerId)
                .orElseThrow(() -> ApiException.forbidden("You are not a member of this circle"));

        if (body.amountMinor() != circle.getContributionMinor()) {
            throw ApiException.businessRule(
                    "Contribution must be exactly " + circle.getContributionMinor() + " (minor units)");
        }

        CircleCycle cycle = currentCycle(circle);
        if (!"open".equals(cycle.getStatus())) {
            throw ApiException.businessRule("The current cycle is not open for contributions");
        }
        if (contributions.findByCycleIdAndMemberId(cycle.getCycleId(), member.getMemberId()).isPresent()) {
            throw ApiException.conflict("You've already contributed this cycle");
        }

        contributions.save(CircleContribution.createPaid(
                cycle.getCycleId(), member.getMemberId(), body.amountMinor()));
        cycle.addToPot(body.amountMinor());
        cycles.save(cycle);

        // Record the member's contribution as a debit on the central ledger.
        String idem = "circle-contrib:" + cycle.getCycleId() + ":" + member.getMemberId();
        ledger.record(callerId, "debit", "transfer", body.amountMinor(), circle.getCurrency(),
                idem, null, null, null);

        return cycleView(circle, cycle);
    }

    /**
     * Guarded payout: only succeeds when every active member has contributed.
     * Pays the scheduled rotation recipient, then advances the circle.
     */
    @Transactional
    public PayoutResponse payout(UUID callerId, UUID circleId) {
        Circle circle = requireCircle(circleId);
        requireOrganiser(circle, callerId);
        if (!"active".equals(circle.getStatus())) {
            throw ApiException.businessRule("Circle is not active");
        }

        CircleCycle cycle = currentCycle(circle);
        List<CircleMember> active = activeMembers(circleId);
        long contributed = contributions.countByCycleId(cycle.getCycleId());

        // The core safety guard: no payout until the pot is complete.
        if (contributed < active.size()) {
            throw ApiException.businessRule(
                    "Payout blocked: " + contributed + " of " + active.size()
                    + " members have contributed. Everyone must pay in first.");
        }
        if (!"open".equals(cycle.getStatus())) {
            throw ApiException.businessRule("This cycle has already paid out");
        }

        CircleMember recipient = active.stream()
                .filter(m -> m.getMemberId().equals(cycle.getPayoutMemberId()))
                .findFirst()
                .orElseThrow(() -> ApiException.businessRule("No scheduled recipient for this cycle"));

        // Pay the pot to the recipient via the central ledger (credit).
        String idem = "circle-payout:" + cycle.getCycleId();
        var txn = ledger.record(recipient.getUserId(), "credit", "transfer",
                cycle.getPotMinor(), circle.getCurrency(), idem, null, null, null);
        cycle.markPaid();
        cycles.save(cycle);

        // Advance: next cycle, or complete when everyone has been paid once.
        int nextNo = cycle.getCycleNumber() + 1;
        if (nextNo > active.size()) {
            circle.complete();
            circles.save(circle);
        } else {
            circle.setCurrentCycleNo(nextNo);
            circles.save(circle);
            openCycle(circle, nextNo);
        }

        return new PayoutResponse(cycle.getCycleId().toString(),
                recipient.getMemberId().toString(), cycle.getPotMinor(),
                txn.getTxnId().toString(), nextNo, circle.getStatus());
    }

    @Transactional(readOnly = true)
    public CircleView get(UUID callerId, UUID circleId) {
        Circle circle = requireCircle(circleId);
        // Only members can view a circle's details (transparent to members).
        if (!members.existsByCircleIdAndUserId(circleId, callerId)) {
            throw ApiException.forbidden("You are not a member of this circle");
        }
        return view(circle);
    }

    @Transactional(readOnly = true)
    public List<CircleView> listForUser(UUID userId) {
        return members.findByUserId(userId).stream()
                .map(m -> circles.findById(m.getCircleId()).orElse(null))
                .filter(c -> c != null)
                .map(this::view)
                .toList();
    }

    // ---- helpers ----

    private void openCycle(Circle circle, int cycleNumber) {
        // ROSCA: cycle N pays the member at rotation position N.
        List<CircleMember> active = activeMembers(circle.getCircleId());
        CircleMember recipient = active.stream()
                .filter(m -> m.getPayoutPosition() == cycleNumber)
                .findFirst()
                .orElse(active.get((cycleNumber - 1) % active.size()));
        cycles.save(CircleCycle.create(circle.getCircleId(), cycleNumber, recipient.getMemberId()));
    }

    private CircleCycle currentCycle(Circle circle) {
        return cycles.findByCircleIdAndCycleNumber(circle.getCircleId(), circle.getCurrentCycleNo())
                .orElseThrow(() -> ApiException.businessRule("No open cycle"));
    }

    private List<CircleMember> activeMembers(UUID circleId) {
        return members.findByCircleIdOrderByPayoutPositionAsc(circleId).stream()
                .filter(m -> "active".equals(m.getStatus()))
                .toList();
    }

    private Circle requireCircle(UUID circleId) {
        return circles.findById(circleId)
                .orElseThrow(() -> ApiException.notFound("Circle not found"));
    }

    private void requireOrganiser(Circle circle, UUID callerId) {
        if (!circle.getOrganiserUserId().equals(callerId)) {
            throw ApiException.forbidden("Only the organiser can do this");
        }
    }

    private CircleView view(Circle circle) {
        List<CircleMember> ms = members.findByCircleIdOrderByPayoutPositionAsc(circle.getCircleId());
        CircleCycle cur = circle.getCurrentCycleNo() > 0
                ? cycles.findByCircleIdAndCycleNumber(circle.getCircleId(), circle.getCurrentCycleNo()).orElse(null)
                : null;

        List<MemberView> memberViews = ms.stream().map(m -> {
            boolean contributed = cur != null &&
                    contributions.findByCycleIdAndMemberId(cur.getCycleId(), m.getMemberId()).isPresent();
            return new MemberView(m.getMemberId().toString(), m.getUserId().toString(),
                    m.getPayoutPosition(), m.getStatus(), contributed);
        }).toList();

        CycleView cycleView = cur == null ? null : cycleView(circle, cur);

        return new CircleView(
                circle.getCircleId().toString(), circle.getName(), circle.getType(),
                circle.getContributionMinor(), circle.getCurrency(), circle.getCadence(),
                circle.getMemberLimit(), ms.size(), circle.getCurrentCycleNo(), circle.getStatus(),
                circle.getOrganiserUserId().toString(), memberViews, cycleView);
    }

    private CycleView cycleView(Circle circle, CircleCycle cycle) {
        int in = (int) contributions.countByCycleId(cycle.getCycleId());
        int total = activeMembers(circle.getCircleId()).size();
        return new CycleView(cycle.getCycleId().toString(), cycle.getCycleNumber(),
                cycle.getPayoutMemberId() == null ? null : cycle.getPayoutMemberId().toString(),
                cycle.getPotMinor(), cycle.getStatus(), in, total);
    }

    // Resolve a display name for ledger views (kept simple for the slice).
    @SuppressWarnings("unused")
    private String displayName(UUID userId) {
        return users.findById(userId).map(User::getDisplayName).orElse("Member");
    }
}
