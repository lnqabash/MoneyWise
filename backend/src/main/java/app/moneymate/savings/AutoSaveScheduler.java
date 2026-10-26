package app.moneymate.savings;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the weekly auto-save sweep. In production this would be a resilient
 * job (Quartz / Azure Functions timer) with per-user cadence; here a simple
 * fixed schedule runs the affordability-aware engine.
 */
@Component
public class AutoSaveScheduler {

    private final AutoSaveService autoSaveService;

    public AutoSaveScheduler(AutoSaveService autoSaveService) {
        this.autoSaveService = autoSaveService;
    }

    // Every Monday at 06:00. (Demo: also exposed via POST /v1/goals/auto-save/run)
    @Scheduled(cron = "0 0 6 * * MON")
    public void weeklySweep() {
        autoSaveService.runAll();
    }
}
