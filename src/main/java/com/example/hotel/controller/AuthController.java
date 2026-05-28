package com.example.hotel.controller;

import com.example.hotel.entity.User;
import com.example.hotel.entity.UserRole;
import com.example.hotel.form.RegisterForm;
import com.example.hotel.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterForm registerForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (userRepository.existsByUsername(registerForm.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Username is already taken.");
        }
        if (userRepository.existsByEmail(registerForm.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "Email is already registered.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        User user = new User();
        user.setUsername(registerForm.getUsername());
        user.setEmail(registerForm.getEmail());
        user.setPassword(passwordEncoder.encode(registerForm.getPassword()));
        user.setRole(UserRole.GUEST);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Registration complete. Please sign in.");
        return "redirect:/login";
    }
}
