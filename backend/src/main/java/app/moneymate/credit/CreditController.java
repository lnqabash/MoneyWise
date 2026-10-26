package app.moneymate.credit;

import app.moneymate.common.security.CurrentUser;
import app.moneymate.credit.CreditReadinessService.ReadinessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/credit-readiness")
public class CreditController {

    private final CreditReadinessService creditService;

    public CreditController(CreditReadinessService creditService) {
        this.creditService = creditService;
    }

    @GetMapping
    public ReadinessResponse assess() {
        return creditService.assess(CurrentUser.id());
    }
}
