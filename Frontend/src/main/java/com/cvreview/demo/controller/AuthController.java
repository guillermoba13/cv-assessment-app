package com.cvreview.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.cvreview.demo.dto.LoginRequestDTO;
import com.cvreview.demo.dto.RegisterRequest;
import com.cvreview.demo.service.AuthService;

import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
public class AuthController {

       @Autowired
    private AuthService authService;

    // Mostrar formulario
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequestDTO());
        return "login";
    }

    // Procesar formulario
    @PostMapping("/login")
    public String loginSubmit(
            @ModelAttribute("loginRequest") LoginRequestDTO loginRequest,
            BindingResult result,
            Model model,
            HttpSession session) {

                
            // validate nulls
            if (loginRequest.getUsername().isEmpty() || loginRequest.getPassword().isEmpty()){
                model.addAttribute("error", "Username and password are required.");
                return "login";
            }

            boolean authenticated = authService.authenticate(loginRequest, session);

            if (authenticated) {
                return "redirect:/home";
            } else {
                model.addAttribute("error", "Username or password incorrectos.");
                return "login";
            }
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute RegisterRequest request, Model model) {
        // validate nulls
        if (request.getUserName().isEmpty() || request.getPassword().isEmpty()){
            model.addAttribute("error", "Username and password are required.");
            return "register";
        }
        boolean success = authService.registerUser(request);
        if (success) {
            model.addAttribute("message", "Usuario registrado correctamente.");
            return "redirect:/login";
        } else {
            model.addAttribute("error", "El usuario ya existe o ocurrió un error al registrar.");
            return "register";
        }
    }

    // Página principal
    @GetMapping("/home")
    public String homePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String token = (String) session.getAttribute("token");

        if (username == null || token == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", username);
        return "home";
    }
    
}
