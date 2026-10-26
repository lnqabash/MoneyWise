package app.moneymate.adapters.mobilemoney;

import java.util.List;

/**
 * Port for mobile-money / bank connectors (M-Pesa, bank payout APIs) — ADR-0005.
 *
 * Phase 2 feature #14: auto-ingest transactions and (later) move real money for
 * savings sweeps and circle payouts. Per-market adapters implement this port.
 * The stub returns empty/failed so the app never pretends money moved.
 */
public interface MobileMoneyPort {

    record ExternalTxn(String externalRef, long amountMinor, String currency,
                       String direction, String description, String date) {}

    record TransferResult(boolean success, String externalRef, String message) {}

    /** Pull recent transactions for a linked account (empty until a real adapter is wired). */
    List<ExternalTxn> fetchRecentTransactions(String accountRef);

    /** Move money out (e.g. a circle payout). Stub reports not-wired. */
    TransferResult transfer(String toAccountRef, long amountMinor, String currency, String idempotencyKey);

    boolean isLive();
}
