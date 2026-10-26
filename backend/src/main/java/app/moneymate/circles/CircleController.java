package app.moneymate.circles;

import app.moneymate.circles.dto.CircleDtos.*;
import app.moneymate.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/circles")
public class CircleController {

    private final CircleService circleService;

    public CircleController(CircleService circleService) {
        this.circleService = circleService;
    }

    @PostMapping
    public ResponseEntity<CircleView> create(@Valid @RequestBody CreateCircleBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(circleService.create(CurrentUser.id(), body));
    }

    @GetMapping
    public List<CircleView> myCircles() {
        return circleService.listForUser(CurrentUser.id());
    }

    @GetMapping("/{circleId}")
    public CircleView get(@PathVariable UUID circleId) {
        return circleService.get(CurrentUser.id(), circleId);
    }

    /** Join a circle. In the demo an organiser can add members by userId. */
    @PostMapping("/{circleId}/members")
    public CircleView join(@PathVariable UUID circleId, @Valid @RequestBody JoinBody body) {
        return circleService.join(circleId, body.userId());
    }

    @PostMapping("/{circleId}/start")
    public CircleView start(@PathVariable UUID circleId) {
        return circleService.start(CurrentUser.id(), circleId);
    }

    @PostMapping("/{circleId}/contributions")
    public CycleView contribute(@PathVariable UUID circleId, @Valid @RequestBody ContributeBody body) {
        return circleService.contribute(CurrentUser.id(), circleId, body);
    }

    @PostMapping("/{circleId}/payout")
    public PayoutResponse payout(@PathVariable UUID circleId) {
        return circleService.payout(CurrentUser.id(), circleId);
    }
}
