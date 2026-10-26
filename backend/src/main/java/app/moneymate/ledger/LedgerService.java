package app.moneymate.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The one place money movements are recorded. Every credit/debit in the system
 * must go through here so the ledger stays the single source of truth.
 *
 * Idempotency: callers pass a stable key; replaying the same key returns the
 * existing entry instead of double-posting (protects vulnerable users from
 * phantom/duplicate debits — see docs/05 saga note).
 */
@Service
public class LedgerService {

    private final TransactionRepository transactions;

    public LedgerService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional
    public Transaction record(UUID userId, String direction, String type, long amountMinor,
                              String currency, String idempotencyKey, UUID goalId,
                              UUID incomeId, UUID expenseId) {
        // Idempotent: if this key was already posted, return the original entry.
        var existing = transactions.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        Transaction txn = Transaction.create(userId, direction, type, amountMinor, currency,
                idempotencyKey, goalId, incomeId, expenseId);
        return transactions.save(txn);
    }

    // Convenience wrappers for the common money events.

    @Transactional
    public Transaction recordIncome(UUID userId, long amountMinor, String currency, UUID incomeId) {
        return record(userId, "credit", "income", amountMinor, currency,
                "income:" + incomeId, null, incomeId, null);
    }

    @Transactional
    public Transaction recordExpense(UUID userId, long amountMinor, String currency, UUID expenseId) {
        return record(userId, "debit", "expense", amountMinor, currency,
                "expense:" + expenseId, null, null, expenseId);
    }

    @Transactional
    public Transaction recordSaveSweep(UUID userId, long amountMinor, String currency,
                                       UUID goalId, String idempotencyKey) {
        return record(userId, "debit", "save_sweep", amountMinor, currency,
                idempotencyKey, goalId, null, null);
    }

    @Transactional
    public Transaction recordWithdrawal(UUID userId, long amountMinor, String currency,
                                        UUID goalId, String idempotencyKey) {
        return record(userId, "credit", "withdrawal", amountMinor, currency,
                idempotencyKey, goalId, null, null);
    }

    @Transactional(readOnly = true)
    public List<Transaction> historyForUser(UUID userId) {
        return transactions.findByUserIdOrderByOccurredAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> historyForGoal(UUID goalId) {
        return transactions.findByGoalIdOrderByOccurredAtDesc(goalId);
    }
}
