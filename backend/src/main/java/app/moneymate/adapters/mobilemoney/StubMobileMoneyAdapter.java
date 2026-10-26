package app.moneymate.adapters.mobilemoney;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default MobileMoneyPort: not connected to any provider. Returns empty reads
 * and failed transfers so the system never falsely claims external money moved.
 * A real adapter (e.g. M-Pesa Daraja, a bank API) replaces it by profile/config.
 */
@Component
class StubMobileMoneyAdapter implements MobileMoneyPort {

    @Override
    public List<ExternalTxn> fetchRecentTransactions(String accountRef) {
        return List.of();
    }

    @Override
    public TransferResult transfer(String toAccountRef, long amountMinor, String currency, String idempotencyKey) {
        return new TransferResult(false, null,
                "Mobile-money provider not configured. Transfer recorded in ledger only.");
    }

    @Override
    public boolean isLive() {
        return false;
    }
}
