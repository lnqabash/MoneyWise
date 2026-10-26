package app.moneymate.adapters.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Default MessagingPort: logs instead of sending. A real SMS adapter (Twilio,
 * Africa's Talking, telco SMPP) would be marked @Primary + @Profile and would
 * take precedence over this stub.
 *
 * This keeps the app fully runnable offline while making the integration seam
 * explicit.
 */
@Component
class LoggingMessagingAdapter implements MessagingPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessagingAdapter.class);

    @Override
    public void sendOtp(String phoneE164, String code) {
        // NOTE: never log the raw code in production; this is a dev stub only.
        log.info("[messaging:stub] Would send OTP to {} (code suppressed in real deployments)", phoneE164);
    }

    @Override
    public void sendMessage(String phoneE164, String message) {
        log.info("[messaging:stub] Would send to {}: {}", phoneE164, message);
    }
}

/**
 * Example of where a real adapter plugs in. Left as documentation so the
 * integration path is obvious; enable by implementing and annotating with
 * @Profile("twilio") + @Primary, and supplying credentials via Key Vault/env.
 */
@Configuration
class MessagingAdapterDocs {
    // @Bean @Profile("twilio") MessagingPort twilioMessaging(TwilioProps p) { return new TwilioMessagingAdapter(p); }
}
