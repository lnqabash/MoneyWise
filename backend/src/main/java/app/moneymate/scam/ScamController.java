package app.moneymate.scam;

import app.moneymate.common.security.CurrentUser;
import app.moneymate.scam.dto.ScamDtos.ActionBody;
import app.moneymate.scam.dto.ScamDtos.ActionResponse;
import app.moneymate.scam.dto.ScamDtos.ScanBody;
import app.moneymate.scam.dto.ScamDtos.ScanResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/scam")
public class ScamController {

    private final ScamService scamService;

    public ScamController(ScamService scamService) {
        this.scamService = scamService;
    }

    @PostMapping("/scan")
    public ScanResponse scan(@Valid @RequestBody ScanBody body) {
        return scamService.scan(CurrentUser.id(), body);
    }

    @PostMapping("/{reportId}/action")
    public ActionResponse recordAction(@PathVariable UUID reportId, @Valid @RequestBody ActionBody body) {
        return scamService.recordAction(CurrentUser.id(), reportId, body.action());
    }
}
