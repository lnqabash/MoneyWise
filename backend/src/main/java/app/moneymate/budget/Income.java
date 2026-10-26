package app.moneymate.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "income")
public class Income {

    @Id
    @Column(name = "income_id")
    private UUID incomeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "capture_method", nullable = false)
    private String captureMethod;

    @Column(name = "is_business", nullable = false)
    private boolean business;

    @Column(name = "received_on", nullable = false)
    private LocalDate receivedOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Income() {}

    public static Income create(UUID userId, long amountMinor, String currency, String source,
                                String captureMethod, boolean business, LocalDate receivedOn) {
        Income i = new Income();
        i.incomeId = UUID.randomUUID();
        i.userId = userId;
        i.amountMinor = amountMinor;
        i.currency = currency;
        i.source = source;
        i.captureMethod = captureMethod;
        i.business = business;
        i.receivedOn = receivedOn;
        i.createdAt = Instant.now();
        return i;
    }

    public UUID getIncomeId() { return incomeId; }
    public long getAmountMinor() { return amountMinor; }
    public LocalDate getReceivedOn() { return receivedOn; }
}
