package com.user.login.login.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.user.login.login.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.user.login.login.model.Users;
import com.user.login.login.dto.*;
import com.user.login.login.security.JwrUtil;
import java.util.Map;


@RestController
@RequestMapping("/api") // ¿Qué es? Es una etiqueta que dice "esta función responde a esta ruta y método HTTP".
public class UserController {
    
    @Autowired //¿Qué es? Es una forma de pedir a Spring: "dame esto, por favor" (Spring entrega el objeto).
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        Users user = userService.registerUser(request.getUserName(), request.getPassword());
        if (user == null) {
            return ResponseEntity.status(400).body(Map.of("error", "El usuario ya existe o no se pudo registrar"));
        }
        UserResponse response = new UserResponse(user.getId(), user.getUserName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody RegisterRequest request) {
        if (userService.loginUser(request.getUserName(), request.getPassword())) {
            String token = JwrUtil.generateToken(request.getUserName());
            return ResponseEntity.ok(Map.of("token", token));
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
    
}