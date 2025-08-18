package com.redmath.jobportal.auth.controller;

import com.redmath.jobportal.auth.dtos.RegisterRequest;
import com.redmath.jobportal.auth.exceptions.UserRegistrationException;
import com.redmath.jobportal.auth.model.AuthProvider;
import com.redmath.jobportal.auth.model.Role;
import com.redmath.jobportal.auth.services.UserService;
import com.redmath.jobportal.exceptions.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (name.isBlank() || email.isBlank() || password.isBlank() || role.isBlank()) {
            model.addAttribute("error", "All fields are required.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("role", role);
            return "register";
        }

        try {
            RegisterRequest request = RegisterRequest.builder()
                    .name(name)
                    .email(email)
                    .password(password)
                    .role(Role.valueOf(role))
                    .provider(AuthProvider.LOCAL)
                    .build();

            userService.register(request);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";

        } catch (DuplicateEmailException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("role", role);
            return "register";
        } catch (UserRegistrationException e) {
            model.addAttribute("error", "Registration failed. Please try again later.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("role", role);
            return "register";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid role selected");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
    }

    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<Map<String, String>> registerApi(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful! Please login."));
    }
}