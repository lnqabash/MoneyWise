package app.moneymate.budget;

import app.moneymate.budget.dto.BudgetDtos.BudgetResponse;
import app.moneymate.budget.dto.BudgetDtos.ExpenseBody;
import app.moneymate.budget.dto.BudgetDtos.ExpenseResponse;
import app.moneymate.budget.dto.BudgetDtos.IncomeBody;
import app.moneymate.budget.dto.BudgetDtos.IncomeResponse;
import app.moneymate.common.security.CurrentUser;
import app.moneymate.profile.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserRepository users;

    public BudgetController(BudgetService budgetService, UserRepository users) {
        this.budgetService = budgetService;
        this.users = users;
    }

    @PostMapping("/income")
    public ResponseEntity<IncomeResponse> recordIncome(@Valid @RequestBody IncomeBody body) {
        var id = budgetService.recordIncome(CurrentUser.id(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IncomeResponse(id.toString(), true));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> recordExpense(@Valid @RequestBody ExpenseBody body) {
        var r = budgetService.recordExpense(CurrentUser.id(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ExpenseResponse(r.expenseId().toString(), r.category(), r.autoCategorised()));
    }

    @GetMapping("/budget/current")
    public BudgetResponse currentBudget() {
        var userId = CurrentUser.id();
        String currency = users.findById(userId).map(u -> u.getCurrency()).orElse("ZAR");
        return budgetService.currentBudget(userId, currency);
    }

    @GetMapping("/budget/forecast")
    public app.moneymate.budget.dto.BudgetDtos.ForecastResponse forecast(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "14") int horizonDays) {
        var userId = CurrentUser.id();
        String currency = users.findById(userId).map(u -> u.getCurrency()).orElse("ZAR");
        return budgetService.forecast(userId, currency, Math.min(60, Math.max(7, horizonDays)));
    }

    @PostMapping("/budget/smooth")
    public app.moneymate.budget.dto.BudgetDtos.SmoothingResponse smooth(
            @RequestBody app.moneymate.budget.dto.BudgetDtos.SmoothingBody body) {
        var userId = CurrentUser.id();
        String currency = body.currency() != null ? body.currency()
                : users.findById(userId).map(u -> u.getCurrency()).orElse("ZAR");
        return budgetService.smooth(currency, body.amountMinor(), body.weeks());
    }
}
