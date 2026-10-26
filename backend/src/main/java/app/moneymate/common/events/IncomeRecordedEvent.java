package app.moneymate.common.events;

import java.util.UUID;

/**
 * Life-event published when income is recorded. In-process stand-in for the
 * Azure Service Bus "income.recorded" topic (docs/05). Subscribers re-forecast
 * the budget, update analytics, etc. Swapping to a real bus later only changes
 * the publish/subscribe transport, not the event contract.
 */
public record IncomeRecordedEvent(UUID userId, UUID incomeId, long amountMinor, String currency) {}
