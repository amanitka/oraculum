package com.oraculum.user.service;

import com.oraculum.user.api.UserManagementApi;
import com.oraculum.user.api.dto.UserDto;
import com.oraculum.user.domain.AuthProvider;
import com.oraculum.user.api.domain.Role;
import com.oraculum.user.domain.UserEntity;
import com.oraculum.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserManagementApi {

    private final UserRepository userRepository;

    public long getUserCount() {
        return userRepository.count();
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public UserEntity registerBootstrapAdmin(String email, String firstName, String lastName, String provider) {
        log.info("System bootstrap: Registering first user {} as ADMIN", email);
        UserEntity adminUser = new UserEntity();
        adminUser.setEmail(email);
        adminUser.setFirstName(firstName);
        adminUser.setLastName(lastName);
        adminUser.setProvider(AuthProvider.fromString(provider));
        adminUser.setRole(Role.ADMIN);
        adminUser.setAnalysisLimit(null); // Unlimited
        adminUser.setEnabled(true);
        adminUser.setLastLoginAt(OffsetDateTime.now());
        
        return userRepository.save(adminUser);
    }

    @Transactional
    public UserEntity updateLoginDetails(UserEntity user, String firstName, String lastName, String provider) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLastLoginAt(OffsetDateTime.now());
        user.setProvider(AuthProvider.fromString(provider));
        
        return userRepository.save(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(
                        u.getId(),
                        u.getEmail(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getDisplayName(),
                        u.getProvider() != null ? u.getProvider().name() : null,
                        u.getRole() != null ? u.getRole().name() : null,
                        u.getAnalysisLimit(),
                        u.isEnabled(),
                        u.getLastLoginAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createOrUpdateUser(UserDto userDto) {
        UserEntity user;
        if (userDto.id() != null) {
            user = userRepository.findById(userDto.id())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userDto.id()));
        } else {
            if (userRepository.findByEmail(userDto.email()).isPresent()) {
                throw new IllegalArgumentException("User with email " + userDto.email() + " already exists");
            }
            log.info("Admin creating new user: {}", userDto.email());
            user = new UserEntity();
            user.setProvider(AuthProvider.PENDING);
        }

        user.setEmail(userDto.email());
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        
        if (userDto.role() != null) {
            user.setRole(Role.valueOf(userDto.role()));
        } else {
            user.setRole(Role.USER);
        }
        
        user.setEnabled(userDto.enabled());
        userRepository.save(user);
    }
}
