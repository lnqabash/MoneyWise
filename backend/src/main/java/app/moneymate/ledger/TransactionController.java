package app.moneymate.ledger;

import app.moneymate.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Read-only view of the user's transaction ledger (docs/07). */
@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

    private final LedgerService ledger;

    public TransactionController(LedgerService ledger) {
        this.ledger = ledger;
    }

    public record TxnView(String txnId, String direction, String type,
                          long amountMinor, String currency, String status, Instant occurredAt) {}

    @GetMapping
    public List<TxnView> list() {
        return ledger.historyForUser(CurrentUser.id()).stream()
                .map(t -> new TxnView(t.getTxnId().toString(), t.getDirection(), t.getType(),
                        t.getAmountMinor(), t.getCurrency(), t.getStatus(), t.getOccurredAt()))
                .toList();
    }
}
