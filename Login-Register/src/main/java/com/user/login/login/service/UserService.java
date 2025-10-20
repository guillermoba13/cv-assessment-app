package com.user.login.login.service;

import com.user.login.login.model.Users;
import com.user.login.login.repository.UserRepository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Users registerUser(String userName, String password) {
         // verificamos si el usuario ya existe
       Optional<Users> existingUser = userRepository.findByUserName(userName);
        if (!existingUser.isEmpty()) {
            // Retorna null o lanza una excepción controlada
            return null;
        }
        // Example user registration logic
        String encodedPassword = passwordEncoder.encode(password);
        Users user = new Users(userName, encodedPassword);
        return userRepository.save(user);
    }

    public boolean loginUser(String userName, String password) {
        // Example user login logic
        return userRepository.findByUserName(userName)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }
}
