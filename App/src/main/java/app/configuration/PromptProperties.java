package app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "prompts")
@PropertySource(value = "classpath:prompts.yml", factory = YamlPropertySourceFactory.class)
public class PromptProperties {

    private Map<String, String> templates = new HashMap<>();

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }
}
