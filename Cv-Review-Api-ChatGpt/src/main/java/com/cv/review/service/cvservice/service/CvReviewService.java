package com.cv.review.service.cvservice.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.cv.review.service.cvservice.client.OpenAiClient;
import com.cv.review.service.cvservice.dto.ReviewResponse;
import com.cv.review.service.cvservice.util.PromptTemplates;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Service that orchestrates CV review:
 * - builds the prompt,
 * - calls the OpenAI client,
 * - parses the JSON response to ReviewResponse.
 *
 * Returns a Mono<ReviewResponse> (reactive) since OpenAiClient is reactive.
 */
@Service
public class CvReviewService {

    private static final Logger log = LoggerFactory.getLogger(CvReviewService.class);

    private final OpenAiClient openAiClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int maxCharacters; // límite de caracteres para el CV

    public CvReviewService(OpenAiClient openAiClient,
                           @Value("${cvreview.openai.model}") String model,
                           @Value("${cvreview.max.chars}") int maxCharacters) {
        this.openAiClient = openAiClient;
        this.model = model;
        this.maxCharacters = maxCharacters;
    }

    /**
     * Checks the text of the CV against the job description.
     *
     * @param cvText             text extracted from the CV
     * @param vacancyDescription job description
     * @return Mono with ReviewResponse (parsed or fallback)
     */
    public Mono<ReviewResponse> reviewText(String cvText, String vacancyDescription) {

        boolean truncated = false;

        if (cvText == null) cvText = "";

        // protect vacancyDescription from null
        if (vacancyDescription == null) vacancyDescription = "";
        
        if (cvText.length() > maxCharacters) {
            log.warn("The CV text was truncated to {} characters to comply with the limit.", maxCharacters);
            cvText = cvText.substring(0, maxCharacters);
            truncated = true;
        }

        if (vacancyDescription.length() > maxCharacters) {
            log.warn("The job description was truncated to {} characters to comply with the limit.", maxCharacters);
            vacancyDescription = vacancyDescription.substring(0, maxCharacters);
            truncated = true;
            
        }

        String prompt = PromptTemplates.buildCvPrompt(cvText, vacancyDescription);
        log.debug("Prompt length: {}", prompt.length());

        boolean finalTruncated = truncated;

        return openAiClient.sendPrompt(model, prompt)
                .map(openAiRaw -> {
                    // We attempt to parse the text returned by OpenAI as JSON to ReviewResponse.
                    try {
                        ReviewResponse parsed = objectMapper.readValue(openAiRaw, ReviewResponse.class);
                        // Guardamos raw por auditoría/debug
                        parsed.setRawOpenAiResponse(openAiRaw);

                        if (finalTruncated) {
                            parsed.setResumen(parsed.getResumen() 
                            + " (Note: The CV or job description has been truncated due to character limits.)");
                        }
                        return parsed;
                    } catch (Exception e) {
                        // If parsing fails, we return an object with fallback and save the raw data.
                        log.warn("Unable to parse OpenAI's response as JSON: {}. Error: {}", openAiRaw, e.getMessage());
                        ReviewResponse fallback = new ReviewResponse();
                        fallback.setApto("no");
                        fallback.setPuntuacion(0);
                        String resumenMsg = "The response from OpenAI could not be parsed.";
                        if (finalTruncated) {
                            resumenMsg += "In addition, the text was truncated to " + maxCharacters + " characters.";
                        }
                        fallback.setResumen(resumenMsg);
                        fallback.setRawOpenAiResponse(openAiRaw);
                        return fallback;
                    }
                })
                .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(err -> {
                            String m = err.getMessage() == null ? "" : err.getMessage();
                            return m.contains("429") || m.contains("Too Many Requests");
                        })
                        .onRetryExhaustedThrow((spec, signal) ->
                            new RuntimeException("OpenAI: Too many requests, please try again later."))
                )
                .doOnError(err -> log.error("Error processing reviewText: {}", err.getMessage(), err));
    }
}
