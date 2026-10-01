package com.thonha.backend.service;

import com.thonha.backend.dto.request.UserCreateRequest;
import com.thonha.backend.dto.request.UserSearchRequest;
import com.thonha.backend.dto.response.UserResponse;
import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.User;
import com.thonha.backend.entity.UserStatus;
import com.thonha.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserService {
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;


    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại");
        }
        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse setStatus(Long id, UserStatus userStatus) {
        User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setStatus(userStatus);
        return toResponse(user);
    }

    public Page<UserResponse> getUsers(UserSearchRequest userSearchRequest, Pageable pageable){
        return userRepository.search(
                userSearchRequest.getKeyword(),
                userSearchRequest.getUserStatus(),
                        userSearchRequest.getRole(),
                userSearchRequest.getCreatedFrom(),
                userSearchRequest.getCreatedTo(),
                pageable)
                .map(this::toResponse);
    }
    private UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                roles,
                user.getAvatar(),
                user.getCreatedAt(),
                user.getStatus().toString()
        );
    }


    public UserResponse getById(Long id) {
        return toResponse(userRepository.findById(id).orElseThrow());
    }

}