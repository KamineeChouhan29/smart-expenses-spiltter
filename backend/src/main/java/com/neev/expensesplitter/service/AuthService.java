package com.neev.expensesplitter.service;

import com.neev.expensesplitter.dto.JwtResponse;
import com.neev.expensesplitter.dto.LoginRequest;
import com.neev.expensesplitter.dto.RegisterRequest;
import com.neev.expensesplitter.model.User;
import com.neev.expensesplitter.repository.UserRepository;
import com.neev.expensesplitter.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public JwtResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new RuntimeException("Username already taken");
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .build();

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());
        return new JwtResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    public JwtResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return new JwtResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }
}
