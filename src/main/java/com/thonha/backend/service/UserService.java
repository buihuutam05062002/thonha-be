package com.thonha.backend.service;

import com.thonha.backend.dto.request.UserSearchRequest;
import com.thonha.backend.dto.response.UserResponse;
import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.UserStatus;
import com.thonha.backend.entity.Users;
import com.thonha.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@FieldDefaults(level =  AccessLevel.PRIVATE)
public class UserService {
     final   UserRepository userRepository;



     @Transactional
    public UserResponse setStatus(Long id,UserStatus userStatus){
        Users user = userRepository.findById(id).orElseThrow(()  -> new EntityNotFoundException("User not found: " + id));
        user.setUserStatus(userStatus);
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
    private  UserResponse toResponse(Users user){
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                roles,
                user.getAvatar(),
                user.getCreatedAt(),
                user.getUserStatus().toString()
        );
    }


    public UserResponse getById(Long id){
        return toResponse(userRepository.findById(id).orElseThrow());
    }


}
