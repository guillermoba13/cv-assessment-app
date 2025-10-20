package com.cvreview.demo.dto;

public class CvReviewResponse {

     private String resumen;
    private int puntuacion;
    private String apto;
    private String raw; // raw JSON por si quieres mostrar en debug

    public CvReviewResponse() {}

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getApto() {
        return apto;
    }

    public void setApto(String apto) {
        this.apto = apto;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

   
}
