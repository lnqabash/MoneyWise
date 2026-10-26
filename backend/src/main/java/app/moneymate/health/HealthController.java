package app.moneymate.health;

import app.moneymate.common.security.CurrentUser;
import app.moneymate.health.dto.HealthDtos.HealthScoreResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/health-score")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public HealthScoreResponse getScore() {
        return healthService.score(CurrentUser.id());
    }
}
