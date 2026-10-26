package app.moneymate.profile;

import app.moneymate.profile.dto.AuthDtos.OtpRequestBody;
import app.moneymate.profile.dto.AuthDtos.OtpRequestResponse;
import app.moneymate.profile.dto.AuthDtos.OtpVerifyBody;
import app.moneymate.profile.dto.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/request")
    public ResponseEntity<OtpRequestResponse> requestOtp(@Valid @RequestBody OtpRequestBody body) {
        var r = authService.requestOtp(body.phone());
        return ResponseEntity.accepted()
                .body(new OtpRequestResponse(r.challengeId(), r.expiresIn(), r.devCode()));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<TokenResponse> verifyOtp(@Valid @RequestBody OtpVerifyBody body) {
        var r = authService.verifyOtp(body.challengeId(), body.code());
        return ResponseEntity.status(HttpStatus.OK).body(new TokenResponse(
                r.accessToken(), "Bearer", r.expiresIn(), r.userId(), r.isNewUser()));
    }
}
