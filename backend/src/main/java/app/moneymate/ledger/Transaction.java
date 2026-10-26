package app.moneymate.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable, append-only ledger entry — the single source of truth for
 * every money movement (docs/06-database-design.md). Corrections are made by
 * writing a reversing entry, never by editing a row.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "txn_id")
    private UUID txnId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "goal_id")
    private UUID goalId;

    @Column(name = "income_id")
    private UUID incomeId;

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "direction", nullable = false)
    private String direction; // credit | debit

    @Column(name = "type", nullable = false)
    private String type; // income | expense | save_sweep | withdrawal | roundup | transfer

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "status", nullable = false)
    private String status; // pending | completed | failed | reversed

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transaction() {}

    public static Transaction create(UUID userId, String direction, String type, long amountMinor,
                                     String currency, String idempotencyKey, UUID goalId,
                                     UUID incomeId, UUID expenseId) {
        Transaction t = new Transaction();
        t.txnId = UUID.randomUUID();
        t.userId = userId;
        t.direction = direction;
        t.type = type;
        t.amountMinor = amountMinor;
        t.currency = currency;
        t.idempotencyKey = idempotencyKey;
        t.goalId = goalId;
        t.incomeId = incomeId;
        t.expenseId = expenseId;
        t.status = "completed";
        t.occurredAt = Instant.now();
        t.createdAt = t.occurredAt;
        return t;
    }

    public UUID getTxnId() { return txnId; }
    public UUID getUserId() { return userId; }
    public UUID getGoalId() { return goalId; }
    public String getDirection() { return direction; }
    public String getType() { return type; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getOccurredAt() { return occurredAt; }
}
