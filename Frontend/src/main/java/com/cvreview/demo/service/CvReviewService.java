package com.cvreview.demo.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;

import com.cvreview.demo.dto.CvReviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


@Service
public class CvReviewService {
    
     @Value("${cvreview.api.base-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CvReviewResponse analyzeCv(MultipartFile file, String token) throws IOException {
        // Preparamos la solicitud al API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null && !token.isBlank()) {
            headers.set("Authorization", "Bearer " + token);
        }

        // Armamos el cuerpo con el archivo
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
              // para evitar problemas, forzamos que tiene longitud
            @Override
            public long contentLength() {
                try {
                    return file.getBytes().length;
                } catch (IOException e) {
                    return 0;
                }
            }
        });

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Hacemos la llamada y pedimos String para parsearlo robustamente
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        String respBody = response.getBody();
        if (respBody == null || respBody.isBlank()) {
            return null;
        }

         // Intentamos mapear la respuesta JSON a CvReviewResponse
        try {
            JsonNode root = objectMapper.readTree(respBody);

            // Si tu cvservice devuelve campos 'resumen', 'puntuacion', 'apto' (según ReviewResponse)
            String resumen = null;
            int puntuacion = 0;
            String apto = null;

            if (root.has("resumen")) {
                resumen = root.get("resumen").asText("");
            } else if (root.has("message")) {
                // fallback: si tu controller envuelve mensaje en 'message'
                resumen = root.get("message").asText("");
            } else if (root.has("rawOpenAiResponse")) {
                // fallback si solo hay raw
                resumen = root.get("rawOpenAiResponse").asText("");
            }

            if (root.has("puntuacion")) {
                puntuacion = root.get("puntuacion").asInt(0);
            } else if (root.has("score")) {
                puntuacion = root.get("score").asInt(0);
            }

            if (root.has("apto")) {
                apto = root.get("apto").asText("");
            }

            CvReviewResponse result = new CvReviewResponse();
            result.setResumen(resumen == null ? "" : resumen);
            result.setPuntuacion(puntuacion);
            result.setApto(apto == null ? "" : apto);
            result.setRaw(respBody);

            return result;
        } catch (Exception e) {
            // Si no es JSON o formato inesperado, devolvemos el raw en message
            CvReviewResponse fallback = new CvReviewResponse();
            fallback.setResumen("Respuesta inesperada del servicio: " + respBody);
            fallback.setPuntuacion(0);
            fallback.setApto("no");
            fallback.setRaw(respBody);
            return fallback;
        }
    }
}
