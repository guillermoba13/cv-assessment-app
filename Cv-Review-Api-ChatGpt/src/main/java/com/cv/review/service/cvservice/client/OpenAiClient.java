package com.cv.review.service.cvservice.client;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    
    private final WebClient webClient;
    private final String apiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor.
     *
     * @param apiUrl  The base URL of the API (e.g., https://api.openai.com/v1/chat/completions).
     * @param apiKey  The Bearer authorization key (injected from environment variables).
     */

    public OpenAiClient(@Value("${cvreview.openai.api-url}") String apiUrl,
                        @Value("${cvreview.openai.api-key}") String apiKey) {
        this.apiUrl = apiUrl;
        this.webClient = WebClient.builder()
                                .baseUrl(apiUrl)
                                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .build();
    }

    /**
     * Sends a prompt to the chat completions endpoint and returns the textual content
     * of the first choice (choice[0].message.content).
     *
     * @param model  The identifier of the model to be used (e.g., “gpt-4o-mini”).
     * @param prompt The text of the prompt to be sent as a role=user message.
     * @return Mono<String> with the text returned by the model (or an error in case of failure).
     */
    public Mono<String> sendPrompt(String model, String prompt) {
        try {
            // We build the JSON structure required by the API:
            // { “model”: “...”, “messages”: [ { “role”: “user”, ‘content’: “...” } ] }
            JsonNode responseBody = objectMapper.createObjectNode()
                    .put("model", model)
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", prompt)));
            
            // POST call and return of the complete JSON as JsonNode
            return webClient.post()
                            .bodyValue(objectMapper.writeValueAsString(responseBody))
                            .retrieve()
                            // Manejo de respuestas HTTP no-ok: convertir a error descriptivo
                            .onStatus(status -> !status.is2xxSuccessful(), this::mapToException)
                            .bodyToMono(JsonNode.class)
                            .map(this::extractFirstChoiceContent)
                            .doOnSuccess(text -> log.debug("OpenAI response content length: {}", text == null ? 0 : text.length()))
                            .doOnError(err -> log.error("Error calling OpenAI: {}", err.getMessage(), err));
        } catch (Exception e) {
            // Serialization or body construction errors
            log.error("Error building request to OpenAI", e);
            return Mono.error(new RuntimeException("Error building request to OpenAI", e));
        }
    }

    /**
     * Extracts the textual content of the first choice from OpenAI's JSON response.
     * If the expected format does not exist, returns the complete JSON as text (fallback).
     *
     * Expected response structure:
     * {
     *   “choices”: [
     *     { “message”: { “role”:“assistant”, ‘content’:“...” }, ... }
     *   ],
     *   ...
     * }
     *
     * @param response JsonNode (parsed response)
     * @return String with the content or the complete JSON if the field was not found.
     */
    private String extractFirstChoiceContent(JsonNode response) {
        try {
            if (response == null) {
                return "";
            }
            JsonNode choices = response.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode first = choices.get(0);
                JsonNode message = first.get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null && !content.isNull()) {
                        return content.asText();
                    }
                }
            }
            // Fallback: return the complete JSON if we don't find the expected field
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("Could not extract choice[0].message.content, returning raw JSON", e);
            try {
                return objectMapper.writeValueAsString(response);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    /**
     * Converts a non-2xx HTTP response into an exception that propagates as a Mono error.
     * Reads the body (if available) to provide more context. clientResponse.statusCode().value(), body);
     */
    private Mono<? extends Throwable> mapToException(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
                .defaultIfEmpty("No body")
                .flatMap(body -> {
                    String msg = String.format("OpenAI API returned status %d: %s",
                            clientResponse.statusCode(), body);
                    log.error(msg);
                    return Mono.error(new RuntimeException(msg));
                });
    }
    
}
