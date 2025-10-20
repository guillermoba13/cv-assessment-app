package com.cv.review.service.cvservice.dto;

import java.util.ArrayList;
import java.util.List;


/**
 * DTO representing the CV review response.
 * Maps the JSON structure we requested from OpenAI.
 */
public class ReviewResponse {

    private String apto; // "yes" o "no" (we follow the format requested in the prompt)
    private Integer puntuacion; // 0-100
    private List<String> competenciasFaltantes = new ArrayList<>();
    private List<String> cursosRecomendados = new ArrayList<>();
    private String resumen;
    private String rawOpenAiResponse; // raw text/json recibido de OpenAI

    
    public String getApto() {
        return apto;
    }

    public void setApto(String apto) {
        this.apto = apto;
    }

    public Integer getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Integer puntuacion) {
        this.puntuacion = puntuacion;
    }

    public List<String> getCompetenciasFaltantes() {
        return competenciasFaltantes;
    }

    public void setCompetenciasFaltantes(List<String> competenciasFaltantes) {
        this.competenciasFaltantes = competenciasFaltantes;
    }

    public List<String> getCursosRecomendados() {
        return cursosRecomendados;
    }

    public void setCursosRecomendados(List<String> cursosRecomendados) {
        this.cursosRecomendados = cursosRecomendados;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getRawOpenAiResponse() {
        return rawOpenAiResponse;
    }

    public void setRawOpenAiResponse(String rawOpenAiResponse) {
        this.rawOpenAiResponse = rawOpenAiResponse;
    }
}
