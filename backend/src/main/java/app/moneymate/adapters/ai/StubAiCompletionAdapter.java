package app.moneymate.adapters.ai;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default AiCompletionPort: a deterministic, offline fallback. It does NOT call
 * any external model, so the app runs fully without cloud credentials. A real
 * Azure OpenAI adapter (marked @Primary + @Profile("azure-openai")) replaces it
 * by providing a competing bean.
 *
 * The stub is honest: it echoes grounded input rather than inventing content,
 * which also keeps it safe from hallucination.
 */
@Component
class StubAiCompletionAdapter implements AiCompletionPort {

    @Override
    public String complete(String groundedPrompt, String language) {
        // Return the grounded prompt as-is: no model, no hallucination.
        return groundedPrompt;
    }

    @Override
    public String translate(String text, String targetLanguage) {
        // No translation service wired; return original text unchanged.
        return text;
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public String summariseReasons(List<String> reasons, String language) {
        return reasons.isEmpty() ? "" : String.join(" ", reasons);
    }
}
