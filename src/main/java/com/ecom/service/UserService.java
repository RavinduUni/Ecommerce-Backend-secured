package com.ecom.service;

import com.ecom.dto.User;
import com.ecom.entity.UserEntity;
import com.ecom.enums.UserRole;
import com.ecom.exception.ApiException;
import com.ecom.mapper.UserMapper;
import com.ecom.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD = 6;

    private final UserRepository repository;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserRepository repository, UserMapper userMapper, AuthenticationManager authenticationManager, JWTService jwtService, BCryptPasswordEncoder encoder) {
        this.repository = repository;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.encoder = encoder;
    }

    public List<User> getAllUsers() {
        return repository.findAll().stream().map(userMapper::toUserDto).toList();
    }

    public User getUserById(Long id) {
        return repository.findById(id)
                .map(userMapper::toUserDto)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }

    public Map<String, Object> createUser(User user) {
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw ApiException.badRequest("Enter a valid email address.");
        }
        if (user.getPassword() == null || user.getPassword().length() < MIN_PASSWORD) {
            throw ApiException.badRequest("Password must be at least " + MIN_PASSWORD + " characters.");
        }
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            throw ApiException.badRequest("First name is required.");
        }
        if (repository.findByEmail(email) != null) {
            throw ApiException.conflict("An account with that email already exists.");
        }

        user.setId(null);
        user.setEmail(email);
        user.setRole(UserRole.CUSTOMER); // never trust a role sent by the client
        user.setPassword(encoder.encode(user.getPassword()));

        UserEntity save = repository.save(userMapper.toUserEntity(user));
        User savedUserDto = userMapper.toUserDto(save);
        String token = jwtService.generateToken(save.getEmail());

        Map<String, Object> register = new HashMap<>();
        register.put("student", savedUserDto);
        register.put("token", token);
        return register;
    }

    /**
     * Partial update. Password and email are never changed here (password would be
     * stored unhashed; email is the JWT subject). Role changes need an admin.
     */
    public void updateUser(Long id, User updatedUser, boolean callerIsAdmin) {
        UserEntity existingUser = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found."));

        updatedUser.setPassword(null);
        updatedUser.setEmail(null);
        if (!callerIsAdmin) {
            updatedUser.setRole(null);
        }

        userMapper.updateUserEntityFromDto(updatedUser, existingUser);
        repository.save(existingUser);
    }

    /** Throws BadCredentialsException (→ 401 via GlobalExceptionHandler) on a wrong email or password. */
    public String login(User user) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
        );
        return jwtService.generateToken(user.getEmail());
    }
}
