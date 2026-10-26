package app.moneymate.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "is_business", nullable = false)
    private boolean business;

    @Column(name = "capture_method", nullable = false)
    private String captureMethod;

    @Column(name = "spent_on", nullable = false)
    private LocalDate spentOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Expense() {}

    public static Expense create(UUID userId, long amountMinor, String currency, String category,
                                 String captureMethod, boolean business, LocalDate spentOn) {
        Expense e = new Expense();
        e.expenseId = UUID.randomUUID();
        e.userId = userId;
        e.amountMinor = amountMinor;
        e.currency = currency;
        e.category = category;
        e.captureMethod = captureMethod;
        e.business = business;
        e.spentOn = spentOn;
        e.createdAt = Instant.now();
        return e;
    }

    public UUID getExpenseId() { return expenseId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCategory() { return category; }
    public LocalDate getSpentOn() { return spentOn; }
}
