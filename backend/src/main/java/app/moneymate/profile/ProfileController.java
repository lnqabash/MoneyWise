package app.moneymate.profile;

import app.moneymate.common.ApiException;
import app.moneymate.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RestController
@RequestMapping("/v1/profile")
public class ProfileController {

    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    public record ProfileResponse(String userId, String displayName, String preferredLanguage,
                                  String preferredChannel, String currency, String incomePattern,
                                  String personaSegment, Map<String, Boolean> consent) {}

    public record ProfilePatch(String displayName, String preferredLanguage,
                               String preferredChannel, String incomePattern,
                               String personaSegment, Boolean consentDataAi) {}

    @GetMapping
    public ProfileResponse getProfile() {
        User u = users.findById(CurrentUser.id())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return toResponse(u);
    }

    @PatchMapping
    @Transactional
    public ProfileResponse patchProfile(@Valid @RequestBody ProfilePatch patch) {
        User u = users.findById(CurrentUser.id())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (patch.displayName() != null) u.setDisplayName(patch.displayName());
        if (patch.preferredLanguage() != null) u.setPreferredLanguage(patch.preferredLanguage());
        if (patch.preferredChannel() != null) u.setPreferredChannel(patch.preferredChannel());
        if (patch.incomePattern() != null) u.setIncomePattern(patch.incomePattern());
        if (patch.personaSegment() != null) u.setPersonaSegment(patch.personaSegment());
        if (patch.consentDataAi() != null) u.setConsentDataAi(patch.consentDataAi());
        u.touch();
        users.save(u);
        return toResponse(u);
    }

    private ProfileResponse toResponse(User u) {
        return new ProfileResponse(
                u.getUserId().toString(), u.getDisplayName(), u.getPreferredLanguage(),
                u.getPreferredChannel(), u.getCurrency(), u.getIncomePattern(),
                u.getPersonaSegment(), Map.of("dataForAi", u.isConsentDataAi()));
    }
}
