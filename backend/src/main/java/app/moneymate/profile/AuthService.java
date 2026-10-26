package app.moneymate.profile;

import app.moneymate.common.ApiException;
import app.moneymate.common.security.JwtService;
import app.moneymate.profile.dto.AuthDtos.OtpRequestResult;
import app.moneymate.profile.dto.AuthDtos.RefreshResponse;
import app.moneymate.profile.dto.AuthDtos.TokenResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phone + OTP authentication with refresh-token rotation and basic rate
 * limiting (auth hardening, docs/09).
 *
 * NOTE: In this slice we do not integrate a real SMS gateway. When
 * moneymate.otp.expose-otp=true (dev only) the generated code is returned
 * in the response so you can test end-to-end. In production this is false
 * and the code is delivered out-of-band via the telco adapter (ADR-0005).
 */
@Service
public class AuthService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_OTP_REQUESTS_PER_WINDOW = 5;
    private static final long RATE_WINDOW_SECONDS = 600; // 10 minutes

    private final SecureRandom random = new SecureRandom();
    // In-memory rate limiter (per phone). Production: Redis-backed at the gateway.
    private final Map<String, Window> otpRequestCounts = new ConcurrentHashMap<>();

    private final UserRepository users;
    private final OtpChallengeRepository otps;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final boolean exposeOtp;
    private final long ttlSeconds;

    public AuthService(UserRepository users, OtpChallengeRepository otps,
                       PasswordEncoder encoder, JwtService jwt, RefreshTokenService refreshTokens,
                       @Value("${moneymate.otp.expose-otp:false}") boolean exposeOtp,
                       @Value("${moneymate.otp.ttl-seconds:300}") long ttlSeconds) {
        this.users = users;
        this.otps = otps;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.exposeOtp = exposeOtp;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public OtpRequestResult requestOtp(String phone) {
        validatePhone(phone);
        enforceRateLimit(phone);

        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpChallenge challenge = OtpChallenge.create(phone, encoder.encode(code), ttlSeconds);
        otps.save(challenge);
        // TODO(phase2): deliver via telco SMS adapter (ADR-0005) instead of exposing in response.
        return new OtpRequestResult(
                challenge.getChallengeId(), ttlSeconds, exposeOtp ? code : null);
    }

    @Transactional
    public TokenResult verifyOtp(UUID challengeId, String code) {
        OtpChallenge challenge = otps.findById(challengeId)
                .orElseThrow(() -> ApiException.unauthenticated("Invalid or expired code"));

        if (challenge.isConsumed() || challenge.isExpired()) {
            throw ApiException.unauthenticated("Invalid or expired code");
        }
        if (challenge.getAttempts() >= MAX_ATTEMPTS) {
            throw ApiException.unauthenticated("Too many attempts. Request a new code.");
        }
        if (!encoder.matches(code, challenge.getCodeHash())) {
            challenge.incrementAttempts();
            otps.save(challenge);
            throw ApiException.unauthenticated("Invalid or expired code");
        }

        challenge.consume();
        otps.save(challenge);

        boolean isNew = !users.existsByPhoneE164(challenge.getPhoneE164());
        User user = users.findByPhoneE164(challenge.getPhoneE164())
                .orElseGet(() -> users.save(User.create(challenge.getPhoneE164(), "ZAR")));

        String accessToken = jwt.issueAccessToken(user.getUserId());
        String refreshToken = refreshTokens.issue(user.getUserId());
        return new TokenResult(accessToken, refreshToken, jwt.ttlSeconds(), user.getUserId(), isNew);
    }

    @Transactional
    public RefreshResponse refresh(String rawRefreshToken) {
        var rotated = refreshTokens.rotate(rawRefreshToken);
        String accessToken = jwt.issueAccessToken(rotated.userId());
        return new RefreshResponse(accessToken, rotated.rawToken(), "Bearer", jwt.ttlSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    private void enforceRateLimit(String phone) {
        long now = Instant.now().getEpochSecond();
        Window w = otpRequestCounts.compute(phone, (k, existing) -> {
            if (existing == null || now - existing.windowStart > RATE_WINDOW_SECONDS) {
                return new Window(now, 1);
            }
            return new Window(existing.windowStart, existing.count + 1);
        });
        if (w.count > MAX_OTP_REQUESTS_PER_WINDOW) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMITED", "Too many code requests. Please wait a few minutes.");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("\\+[1-9]\\d{7,14}")) {
            throw ApiException.validation("phone must be E.164 format, e.g. +27831234567");
        }
    }

    private record Window(long windowStart, int count) {}
}
