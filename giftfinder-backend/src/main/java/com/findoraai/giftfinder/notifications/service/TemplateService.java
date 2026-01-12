package com.findoraai.giftfinder.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class TemplateService {

    /**
     * Process a template by replacing placeholders with actual values
     * Supports Mustache-style placeholders: {{variable}}
     */
    public String processTemplate(String templateName, Map<String, Object> data) {
        try {
            String template = loadTemplate(templateName);
            return replacePlaceholders(template, data);
        } catch (IOException e) {
            log.error("Failed to process template {}: {}", templateName, e.getMessage());
            return "<html><body><p>Error loading template</p></body></html>";
        }
    }

    private String loadTemplate(String templateName) throws IOException {
        String path = "templates/email/" + templateName + ".html";
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String replacePlaceholders(String template, Map<String, Object> data) {
        String result = template;
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            
            // Replace {{key}} with value
            result = result.replace("{{" + key + "}}", value);
            
            // Handle conditional blocks: {{#key}}...{{/key}}
            String conditionalStart = "{{#" + key + "}}";
            String conditionalEnd = "{{/" + key + "}}";
            
            if (entry.getValue() != null && !value.isEmpty()) {
                // Keep the content, remove the tags
                result = result.replace(conditionalStart, "");
                result = result.replace(conditionalEnd, "");
            } else {
                // Remove the entire conditional block
                result = removeConditionalBlock(result, conditionalStart, conditionalEnd);
            }
        }
        
        // Remove any remaining placeholders
        result = result.replaceAll("\\{\\{[^}]+\\}\\}", "");
        
        return result;
    }

    private String removeConditionalBlock(String text, String start, String end) {
        int startIdx = text.indexOf(start);
        while (startIdx != -1) {
            int endIdx = text.indexOf(end, startIdx);
            if (endIdx == -1) break;
            
            text = text.substring(0, startIdx) + text.substring(endIdx + end.length());
            startIdx = text.indexOf(start);
        }
        return text;
    }
}
