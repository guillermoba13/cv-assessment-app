package com.cv.review.service.cvservice.controller;


import com.cv.review.service.cvservice.dto.ReviewResponse;
import com.cv.review.service.cvservice.service.CvReviewService;
import com.cv.review.service.cvservice.util.TextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

/**
 * REST controller that exposes the endpoint for uploading CVs and requesting reviews.
 *
 * Main endpoint:
 * POST /api/v1/reviews
 * Form data:
 *  - file: CV file (pdf/docx/txt)
 *  - vacancyDescription: (optional) text with the job description
 *
 * Response: JSON with the ReviewResponse structure
 */
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final CvReviewService service;

    public ReviewController(CvReviewService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ReviewResponse>> reviewCv(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "vacancyDescription", required = false) String vacancyDescription) {

        // Extract text from the file
        String text = TextExtractor.extractText(file);
        if (text.isEmpty()) {
            // Quick response if no text was extracted
            ReviewResponse response = new ReviewResponse();
            response.setApto("no");
            response.setPuntuacion(0);
            response.setResumen("Text cannot be extracted from the selected file.");
            response.setRawOpenAiResponse("");
            return Mono.just(ResponseEntity.badRequest().body(response));
        }

        log.info("File received. Length of text extracted: {}", text.length());

        // Call the service that builds the prompt and query OpenAI
        return service.reviewText(text, vacancyDescription)
            .map(ResponseEntity::ok)
            .onErrorResume(ex -> {
                log.error("Error processing reviewCv: {}", ex.getMessage(), ex);
                ReviewResponse response = new ReviewResponse();
                response.setApto("no");
                response.setPuntuacion(0);
                response.setResumen("Internal error while processing the CV: " + ex.getMessage());
                response.setRawOpenAiResponse("");
                return Mono.just(ResponseEntity.status(500).body(response));
            });
    }
}
