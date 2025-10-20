package com.cv.review.service.cvservice.util;


import org.apache.commons.text.StringEscapeUtils;

/**
 * Plantillas de prompt para construir la instrucción enviada a OpenAI.
 * Aquí pedimos que la respuesta sea exclusivamente JSON válido con campos concretos,
 * para facilitar el parseo en el backend.
 */
public class PromptTemplates {

    /**
     * Construye un prompt seguro (escapando comillas) que solicita una respuesta JSON.
     *
     * @param cvText             Texto extraído del CV (ya puro texto).
     * @param vacancyDescription Descripción de la vacante.
     * @return prompt completo a enviar a OpenAI
     */
    public static String buildCvPrompt(String cvText, String vacancyDescription) {
        String safeCv = StringEscapeUtils.escapeEcmaScript(cvText == null ? "" : cvText);
        String safeVacancy = StringEscapeUtils.escapeEcmaScript(vacancyDescription == null ? "" : vacancyDescription);
        String template = ""
                + "Eres un experto reclutador. Evalúa el siguiente CV respecto a la vacante proporcionada.\n\n"
                + "RESPONDE SOLO con un JSON válido (sin texto adicional) con los campos EXACTOS:\n"
                + "{\n"
                + "  \"apto\": \"yes\" o \"no\",\n"
                + "  \"puntuacion\": entero entre 0 y 100,\n"
                + "  \"competenciasFaltantes\": [array de strings],\n"
                + "  \"cursosRecomendados\": [array de strings],\n"
                + "  \"resumen\": \"justificación breve\"\n"
                + "}\n\n"
                + "CV_TEXT: \"%s\"\n\n"
                + "VACANTE: \"%s\"\n\n"
                + "Si no puedes determinar algún campo, devuélvelo con un valor por defecto (por ejemplo puntuacion: 0 o array vacío).\n"
                + "No incluyas comentarios, explicaciones ni texto fuera del JSON.\n";

        return String.format(template, safeCv, safeVacancy);
    }
}
