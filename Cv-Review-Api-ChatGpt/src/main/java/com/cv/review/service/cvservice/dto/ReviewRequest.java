package com.cv.review.service.cvservice.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for review requests. In the controller, we use MultipartFile directly,
 * but I'm leaving the DTO in case you want to map/validate the request later on.
 */
public class ReviewRequest {
    private MultipartFile file;
    private String vacancyDescription;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getVacancyDescription() {
        return vacancyDescription;
    }

    public void setVacancyDescription(String vacancyDescription) {
        this.vacancyDescription = vacancyDescription;
    }
}

