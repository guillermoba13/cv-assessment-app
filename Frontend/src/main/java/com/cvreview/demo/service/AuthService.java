package com.cvreview.demo.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.cvreview.demo.dto.LoginRequestDTO;
import com.cvreview.demo.dto.RegisterRequest;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Value("${login.api.base-url}")
    private String loginApiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean authenticate(LoginRequestDTO loginRequest, HttpSession session) {
        String url = loginApiBaseUrl + "/login";

       try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> body  = new HashMap<>();
            body .put("userName", loginRequest.getUsername());
            body .put("password", loginRequest.getPassword());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("token")) {
                    String token = responseBody.get("token").toString();
                    session.setAttribute("token", token); // guardamos el JWT
                    session.setAttribute("username", loginRequest.getUsername());
                    return true;
                }
            }

        } catch (HttpClientErrorException e) {
            System.out.println("Error de autenticación: " + e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Si algo falla o credenciales inválidas
    }

    public boolean registerUser(RegisterRequest request) {
        String url = loginApiBaseUrl + "/register"; // http://localhost:8080/api/register
         try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("userName", request.getUserName());
            body.put("password", request.getPassword());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }else if (response.getStatusCode().value() == 409) {
                System.out.println("⚠️ Usuario ya registrado.");
            }

        } catch (Exception e) {
            System.err.println("❌ Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
