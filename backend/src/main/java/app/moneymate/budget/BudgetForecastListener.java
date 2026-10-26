package app.moneymate.budget;

import app.moneymate.common.events.IncomeRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subscribes to income.recorded and refreshes the user's safe-to-spend
 * forecast (Journey A: income-event-triggered re-forecast, not calendar-month).
 *
 * In the slice the forecast is computed on read, so here we simply log the
 * trigger and warm it; the hook is the extension point where a cached
 * forecast or a push notification would be produced.
 */
@Component
public class BudgetForecastListener {

    private static final Logger log = LoggerFactory.getLogger(BudgetForecastListener.class);

    private final BudgetService budgetService;

    public BudgetForecastListener(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIncomeRecorded(IncomeRecordedEvent event) {
        var budget = budgetService.currentBudget(event.userId(), event.currency());
        log.info("Re-forecast after income {} for user {}: safe-to-spend now {} {}",
                event.incomeId(), event.userId(),
                budget.safeToSpendTodayMinor(), budget.currency());
    }
}
