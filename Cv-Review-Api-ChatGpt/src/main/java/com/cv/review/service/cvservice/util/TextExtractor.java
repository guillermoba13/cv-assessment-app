package com.cv.review.service.cvservice.util;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Utilidad para extraer texto de archivos (pdf, docx, txt, etc.) usando Apache Tika.
 * Es síncrona y está pensada para extracciones relativamente cortas (CVs).
 */
public class TextExtractor {
    private static final Logger log = LoggerFactory.getLogger(TextExtractor.class);
    private static final Tika tika = new Tika();

    /**
     * Extrae el texto de un MultipartFile.
     *
     * @param file archivo subido
     * @return texto extraído (o cadena vacía si falla)
     */
    public static String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try (InputStream is = file.getInputStream()) {
            String txt = tika.parseToString(is);
            return txt == null ? "" : txt.trim();
        } catch (Exception e) {
            log.error("Error extracting text from the file: {}", e.getMessage(), e);
            return "";
        }
    }
}
