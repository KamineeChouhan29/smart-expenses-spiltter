package com.neev.expensesplitter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class AIService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AIService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    // ── Categorization ────────────────────────────────────────────────────────

    /**
     * Calls Groq to classify an expense description into one of:
     * Food, Travel, Rent, Shopping, Other
     * Returns "Other" on any failure.
     */
    public String categorizeExpense(String description) {
        String systemPrompt = """
                You are an expense categorization assistant. Given an expense description, \
                respond ONLY with a valid JSON object and nothing else. \
                Example: {"category": "Food"}. \
                The category must be exactly one of: Food, Travel, Rent, Shopping, Other. \
                No explanation, no markdown, just the JSON object.""";

        String userMessage = "Categorize this expense: " + description;

        try {
            String content = callGroq(systemPrompt, userMessage);
            JsonNode json = objectMapper.readTree(content);
            String category = json.get("category").asText("Other");
            List<String> valid = List.of("Food", "Travel", "Rent", "Shopping", "Other");
            return valid.contains(category) ? category : "Other";
        } catch (Exception e) {
            log.warn("Groq categorization failed for '{}': {}", description, e.getMessage());
            return "Other";
        }
    }

    // ── Insights ─────────────────────────────────────────────────────────────

    /**
     * Compares this-week vs last-week spending and returns 2-3 insight strings.
     */
    public List<String> generateInsights(Map<String, BigDecimal> thisWeek,
                                         Map<String, BigDecimal> lastWeek) {
        String systemPrompt = """
                You are a financial insights assistant working in INR (Indian Rupees ₹). \
                Given weekly expense data grouped by category, respond ONLY with a valid JSON \
                object like: {"insights": ["insight1", "insight2"]}. \
                Provide 2-3 short, actionable insights comparing current vs previous week spending. \
                Use the ₹ symbol for amounts. No explanation, no markdown, just the JSON object.""";

        String userMessage = String.format(
                "Current week spending: %s\nPrevious week spending: %s",
                thisWeek, lastWeek);

        try {
            String content = callGroq(systemPrompt, userMessage);
            JsonNode json = objectMapper.readTree(content);
            JsonNode insightsNode = json.get("insights");
            List<String> insights = new ArrayList<>();
            insightsNode.forEach(n -> insights.add(n.asText()));
            return insights;
        } catch (Exception e) {
            log.warn("Groq insights failed: {}", e.getMessage());
            return List.of("Unable to generate insights at this time. Please try again later.");
        }
    }

    // ── Private helper ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGroq(String systemPrompt, String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", groqModel);
        body.put("temperature", 0.1);
        body.put("max_tokens", 300);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userMessage)
        ));

        Map<String, Object> response = webClient.post()
                .uri(groqApiUrl)
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) throw new RuntimeException("Empty response from Groq");

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
