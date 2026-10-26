package app.moneymate.adapters.ai;

import java.util.List;

/**
 * Port for LLM-backed text generation / translation — ADR-0005.
 *
 * Used for coaching copy, explanation phrasing, and localisation. The core
 * depends on this interface; the real adapter wraps Azure OpenAI (behind a
 * private endpoint, with PII redaction guardrails per docs/08). Until wired,
 * the stub returns a deterministic, grounded fallback so nothing breaks.
 */
public interface AiCompletionPort {

    /** Rephrase/expand a grounded prompt into user-facing copy in the given language. */
    String complete(String groundedPrompt, String language);

    /** Translate text into the target language code (ISO-639). */
    String translate(String text, String targetLanguage);

    /** Whether a real model is wired (false for the stub). */
    boolean isLive();

    /** Turn structured reasons into one friendly sentence (explainability layer). */
    default String summariseReasons(List<String> reasons, String language) {
        return reasons.isEmpty() ? "" : String.join(" ", reasons);
    }
}
