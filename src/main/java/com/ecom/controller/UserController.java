package com.ecom.controller;

import com.ecom.dto.User;
import com.ecom.dto.UserPrincipal;
import com.ecom.exception.ApiException;
import com.ecom.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Admin only (see SecurityConfig). */
    @GetMapping("/")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** The signed-in user, resolved from the JWT. */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getUserById(principal.getId()));
    }

    /** Your own account, or any account for an admin. */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        requireSelfOrAdmin(id, principal);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        return new ResponseEntity<>(userService.createUser(user), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        return new ResponseEntity<>(userService.login(user), HttpStatus.CREATED);
    }

    /** Your own account, or any account for an admin. Only admins can change roles. */
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody User updatedUser,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        requireSelfOrAdmin(id, principal);
        userService.updateUser(id, updatedUser, principal.isAdmin());
        return ResponseEntity.ok("User updated successfully");
    }

    private void requireSelfOrAdmin(Long id, UserPrincipal principal) {
        if (!principal.isAdmin() && !principal.getId().equals(id)) {
            throw ApiException.forbidden("You can only view or change your own account.");
        }
    }
}
