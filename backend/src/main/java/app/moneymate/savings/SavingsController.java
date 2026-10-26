package app.moneymate.savings;

import app.moneymate.common.security.CurrentUser;
import app.moneymate.savings.dto.SavingsDtos.ContributionBody;
import app.moneymate.savings.dto.SavingsDtos.ContributionResponse;
import app.moneymate.savings.dto.SavingsDtos.GoalBody;
import app.moneymate.savings.dto.SavingsDtos.GoalResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/goals")
public class SavingsController {

    private final SavingsService savingsService;

    public SavingsController(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsService.createGoal(CurrentUser.id(), body));
    }

    @GetMapping
    public List<GoalResponse> list() {
        return savingsService.listGoals(CurrentUser.id());
    }

    @GetMapping("/{goalId}")
    public GoalResponse get(@PathVariable UUID goalId) {
        return savingsService.getGoal(CurrentUser.id(), goalId);
    }

    @PostMapping("/{goalId}/contributions")
    public ResponseEntity<ContributionResponse> contribute(
            @PathVariable UUID goalId, @Valid @RequestBody ContributionBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsService.contribute(CurrentUser.id(), goalId, body));
    }
}
