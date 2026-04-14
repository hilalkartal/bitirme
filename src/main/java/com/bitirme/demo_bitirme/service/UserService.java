package com.bitirme.demo_bitirme.service;

import com.bitirme.demo_bitirme.data.dto.UserDTO;
import com.bitirme.demo_bitirme.data.entity.AppUser;
import com.bitirme.demo_bitirme.data.mapper.UserMapper;
import com.bitirme.demo_bitirme.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final UserMapper userMapper;

    public UserDTO createUser(String username, String displayName) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        String uname = username.trim();
        String dname = (displayName == null || displayName.isBlank()) ? uname : displayName.trim();

        if (appUserRepository.existsByUsername(uname)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username already taken: " + uname);
        }
        AppUser u = new AppUser();
        u.setUsername(uname);
        u.setDisplayName(dname);
        AppUser saved = appUserRepository.save(u);
        log.info("Created user {} (id={})", saved.getUsername(), saved.getId());
        return userMapper.toUserDTO(saved);
    }

    public List<UserDTO> listUsers() {
        return appUserRepository.findAll().stream()
                .map(userMapper::toUserDTO)
                .toList();
    }

    public AppUser requireUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    public AppUser requireByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + username));
    }
}
