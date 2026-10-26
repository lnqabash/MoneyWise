package app.moneymate.savings;

import app.moneymate.common.security.CurrentUser;
import app.moneymate.savings.dto.SavingsDtos.EmergencyFundResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/emergency-fund")
public class EmergencyFundController {

    private final SavingsService savingsService;

    public EmergencyFundController(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    @GetMapping
    public EmergencyFundResponse get() {
        return savingsService.emergencyFund(CurrentUser.id());
    }
}
