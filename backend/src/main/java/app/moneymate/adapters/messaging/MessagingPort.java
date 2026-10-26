package app.moneymate.adapters.messaging;

/**
 * Port for outbound messaging (SMS / WhatsApp) — ADR-0005.
 *
 * The core depends on this interface, never on a concrete telco. Swap in a
 * real adapter (Twilio, Africa's Talking, Clickatell, a telco SMPP gateway)
 * by providing a bean that implements this port; no core code changes.
 */
public interface MessagingPort {

    /** Deliver a one-time passcode to the given E.164 phone number. */
    void sendOtp(String phoneE164, String code);

    /** Deliver a general notification (e.g. savings confirmation, scam warning). */
    void sendMessage(String phoneE164, String message);
}
