package devlog.devlog.ai;

import devlog.devlog.ai.dto.ChatRequest;
import devlog.devlog.ai.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
public class GroqClient {

    private final GroqProperties props;
    private final RestClient restClient;

    public GroqClient(GroqProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    public String complete(String prompt) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            log.warn("Groq API key is not configured. Set groq.api-key in application-dev.properties.");
            return "AI features require a valid Groq API key.";
        }

        ChatRequest request = new ChatRequest(
                props.getModel(),
                List.of(new ChatRequest.Message("user", prompt)),
                props.getMaxTokens(),
                props.getTemperature()
        );

        try {
            ChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response != null && !response.choices().isEmpty()) {
                return response.choices().getFirst().message().content().trim();
            }
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
        }
        return "AI summary temporarily unavailable";
    }
}
