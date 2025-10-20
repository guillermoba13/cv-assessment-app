package com.cvreview.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.cvreview.demo.dto.CvReviewResponse;

import org.springframework.ui.Model;

import jakarta.servlet.http.HttpSession;
import com.cvreview.demo.service.CvReviewService;

@Controller
public class HomeController {

    @Autowired
    private CvReviewService cvReviewService;

    // public String home 
    @GetMapping("/pageHome")
    public String home(HttpSession session, Model model) {
        Object user = session.getAttribute("username");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", user);
        return "home";
    }  
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // borra todos los datos de la sesión
        return "redirect:/login";
    }

    @PostMapping("/analyze-cv")
    public String analyzeCv(@RequestParam("file") MultipartFile file,
                        Model model,
                        HttpSession session) {
        try {

            // Verificamos si el usuario está logueado
            Object user = session.getAttribute("username");
            Object token = session.getAttribute("token");
            if (user == null || token == null) {
                return "redirect:/login";
            }

            CvReviewResponse response = cvReviewService.analyzeCv(file, token.toString());
            model.addAttribute("username", user.toString());
            if (response != null) {
                model.addAttribute("result", response);
            } else {
                model.addAttribute("error", "No se obtuvo respuesta válida del servicio de revisión.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al analizar el CV: " + e.getMessage());
        }
        return "home";
    }
}
