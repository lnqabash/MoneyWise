package app.moneymate;

import app.moneymate.budget.Expense;
import app.moneymate.budget.ExpenseRepository;
import app.moneymate.budget.Income;
import app.moneymate.budget.IncomeRepository;
import app.moneymate.profile.User;
import app.moneymate.profile.UserRepository;
import app.moneymate.savings.SavingsGoal;
import app.moneymate.savings.SavingsGoalRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Seeds the "Thandi" demo persona (docs/02) so /budget/current returns real
 * numbers out of the box. Runs only when the "seed" profile is active
 * (enabled by default alongside H2) and only if the user doesn't already exist.
 *
 * Uses the domain factories; sets the fixed demo id via reflection so the
 * entities stay otherwise immutable from the outside.
 */
@Component
@Profile("seed")
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_PHONE = "+27830000001";

    private final UserRepository users;
    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;
    private final SavingsGoalRepository goals;

    public DemoDataSeeder(UserRepository users, IncomeRepository incomes,
                          ExpenseRepository expenses, SavingsGoalRepository goals) {
        this.users = users;
        this.incomes = incomes;
        this.expenses = expenses;
        this.goals = goals;
    }

    @Override
    public void run(String... args) {
        if (users.existsByPhoneE164(DEMO_PHONE)) return;

        User thandi = User.create(DEMO_PHONE, "ZAR");
        thandi.setDisplayName("Thandi");
        thandi.setPreferredLanguage("zu");
        thandi.setPersonaSegment("gig_worker");
        thandi.setIncomePattern("daily");
        thandi.setConsentDataAi(true);
        users.save(thandi);
        UUID uid = thandi.getUserId();

        LocalDate today = LocalDate.now();
        // Uneven gig income over recent days.
        incomes.save(Income.create(uid, 42000, "ZAR", "gig", "manual", false, today.minusDays(1)));
        incomes.save(Income.create(uid, 18000, "ZAR", "gig", "manual", false, today.minusDays(2)));
        incomes.save(Income.create(uid, 65000, "ZAR", "gig", "manual", false, today.minusDays(3)));
        incomes.save(Income.create(uid, 30000, "ZAR", "gig", "manual", false, today.minusDays(5)));
        incomes.save(Income.create(uid, 51000, "ZAR", "gig", "manual", false, today.minusDays(8)));

        expenses.save(Expense.create(uid, 12000, "ZAR", "fuel", "manual", false, today.minusDays(1)));
        expenses.save(Expense.create(uid, 8000, "ZAR", "food", "manual", false, today.minusDays(2)));
        expenses.save(Expense.create(uid, 5000, "ZAR", "data", "manual", false, today.minusDays(3)));

        SavingsGoal carRepair = SavingsGoal.create(uid, "Car Repair", "repair", 200000, "ZAR",
                today.plusMonths(3), "good_day", 2000L);
        carRepair.contribute(46000);
        goals.save(carRepair);

        // Two more members so Community Circles can be demoed end to end.
        seedMember("+27830000002", "Grace", "zu");
        seedMember("+27830000003", "Maria", "zu");
    }

    private void seedMember(String phone, String name, String language) {
        if (users.existsByPhoneE164(phone)) return;
        User u = User.create(phone, "ZAR");
        u.setDisplayName(name);
        u.setPreferredLanguage(language);
        users.save(u);
    }
}
