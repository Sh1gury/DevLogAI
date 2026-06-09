package devlog.devlog.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "groq")
@Data
public class GroqProperties {
    private String apiKey = "";
    private String model = "llama-3.3-70b-versatile";
    private String baseUrl = "https://api.groq.com/openai/v1";
    private int maxTokens = 1024;
    private double temperature = 0.7;
}
