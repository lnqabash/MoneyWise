package app.moneymate.savings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @Column(name = "goal_id")
    private UUID goalId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "goal_type", nullable = false)
    private String goalType = "general";

    @Column(name = "target_minor", nullable = false)
    private long targetMinor;

    @Column(name = "saved_minor", nullable = false)
    private long savedMinor = 0;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "rule_type", nullable = false)
    private String ruleType = "manual";

    @Column(name = "rule_amount_minor")
    private Long ruleAmountMinor;

    @Column(name = "soft_locked", nullable = false)
    private boolean softLocked = true;

    @Column(name = "status", nullable = false)
    private String status = "active";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavingsGoal() {}

    public static SavingsGoal create(UUID userId, String name, String goalType, long targetMinor,
                                     String currency, LocalDate targetDate, String ruleType,
                                     Long ruleAmountMinor) {
        SavingsGoal g = new SavingsGoal();
        g.goalId = UUID.randomUUID();
        g.userId = userId;
        g.name = name;
        g.goalType = goalType == null ? "general" : goalType;
        g.targetMinor = targetMinor;
        g.currency = currency;
        g.targetDate = targetDate;
        g.ruleType = ruleType == null ? "manual" : ruleType;
        g.ruleAmountMinor = ruleAmountMinor;
        g.createdAt = Instant.now();
        return g;
    }

    /** Adds a contribution, capping at target and marking reached when full. */
    public void contribute(long amountMinor) {
        this.savedMinor = Math.min(targetMinor, this.savedMinor + amountMinor);
        if (this.savedMinor >= this.targetMinor) {
            this.status = "reached";
        }
    }

    public double progress() {
        return targetMinor == 0 ? 0 : (double) savedMinor / targetMinor;
    }

    public UUID getGoalId() { return goalId; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getGoalType() { return goalType; }
    public long getTargetMinor() { return targetMinor; }
    public long getSavedMinor() { return savedMinor; }
    public String getCurrency() { return currency; }
    public LocalDate getTargetDate() { return targetDate; }
    public String getRuleType() { return ruleType; }
    public Long getRuleAmountMinor() { return ruleAmountMinor; }
    public boolean isSoftLocked() { return softLocked; }
    public String getStatus() { return status; }
}
