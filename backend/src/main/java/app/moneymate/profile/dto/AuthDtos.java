package app.moneymate.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/** Request/response payloads for the authentication APIs (docs/07 §1). */
public final class AuthDtos {

    private AuthDtos() {}

    public record OtpRequestBody(
            @NotBlank @Pattern(regexp = "\\+[1-9]\\d{7,14}",
                    message = "must be E.164, e.g. +27831234567") String phone,
            String channel) {}

    public record OtpRequestResponse(UUID challengeId, long expiresIn, String devCode) {}

    public record OtpVerifyBody(
            @NotNull UUID challengeId,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String code) {}

    public record TokenResponse(
            String accessToken, String tokenType, long expiresIn,
            UUID userId, boolean isNewUser) {}

    // Internal service results
    public record OtpRequestResult(UUID challengeId, long expiresIn, String devCode) {}
    public record TokenResult(String accessToken, long expiresIn, UUID userId, boolean isNewUser) {}
}
