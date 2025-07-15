package com.redmath.newsapp.user;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepo;

    @GetMapping("/me")
    public ResponseEntity<User> getLoggedInUser(){

        User currentUser=CurrentUser.get();
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/editors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllEditors(@RequestParam(defaultValue = "EDITOR") Role role) {
        return ResponseEntity.ok(userRepo.findAll()
                .stream()
                .filter(user -> user.getRole().equals(role))
                .toList());
    }


}
