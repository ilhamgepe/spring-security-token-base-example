package com.gepe.bayr.user.internal.service;


import com.gepe.bayr.shared.constants.CacheNames;
import com.gepe.bayr.shared.exception.ServiceException;
import com.gepe.bayr.user.api.UserService;
import com.gepe.bayr.user.api.dto.RoleDto;
import com.gepe.bayr.user.api.dto.UserDto;
import com.gepe.bayr.user.api.dto.UserProfileDto;
import com.gepe.bayr.user.api.type.UserStatusType;
import com.gepe.bayr.user.exception.RoleError;
import com.gepe.bayr.user.exception.UserError;
import com.gepe.bayr.user.internal.entity.Role;
import com.gepe.bayr.user.internal.entity.User;
import com.gepe.bayr.user.internal.entity.UserProfile;
import com.gepe.bayr.user.internal.repo.RoleRepo;
import com.gepe.bayr.user.internal.repo.UserRepo;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNickname(String nickname) {
        return userRepo.existsByNickname(nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return userRepo.findById(id).map(
                user -> new UserDto(
                        user.getId(), user.getEmail(),
                        user.getNickname(), user.getStatus(), user.getKycStatus(), toProfileDto(user.getUserProfile()), toRoleDto(user.getRoles())
                )).orElseThrow(() -> new ServiceException(UserError.NOT_FOUND));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.USER_DETAILS, key = "#id")
    @Transactional(readOnly = true)
    public UserDto getByIdDetails(UUID id) {
        return userRepo.findByIdWithDetails(id).map(this::toUserDto)
                .orElseThrow(() -> new ServiceException(UserError.NOT_FOUND));
    }

    /**
     * register user, create user, user profile, dan assign role 'USER'
     *
     * @param email
     * @param nickname
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDto registerUser(String email, String nickname) {
        if (userRepo.existsByEmail(email)) {
            throw new ServiceException(UserError.EMAIL_ALREADY_EXISTS,email);
        }
        if (userRepo.existsByNickname(nickname)) {
            throw new ServiceException(UserError.NICKNAME_ALREADY_EXISTS,nickname);
        }
        UUID id = UuidCreator.getTimeOrderedEpoch();
        String streamKey = generateStreamKey();

        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setStatus(UserStatusType.ACTIVE);
        user.setCreatedAt(Instant.now());


        // register user profile
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(user.getId());
        userProfile.setStreamKey(streamKey);
        userProfile.setMessageForSupporters("Thank you for your support!");

        user.setUserProfile(userProfile);

        // set role
        Role defaultRole = roleRepo.findByCode("USER")
                .orElseThrow(() -> new ServiceException(RoleError.NOT_FOUND));


        user.addRole(defaultRole);

        // save user and user profile
        userRepo.save(user);


        return toUserDto(user);
    }

    private String generateStreamKey() {
        UUID uuid = UuidCreator.getTimeOrderedEpoch();
        return "live_" + uuid.toString().replace("-", "");
    }

    private UserDto toUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getKycStatus(),
                toProfileDto(user.getUserProfile()),
                toRoleDto(user.getRoles())
        );
    }

    private Set<RoleDto> toRoleDto(Set<Role> roles) {
        return roles.stream()
                .map(role -> new RoleDto(
                        role.getId(),
                        role.getCode(),
                        role.getName(),
                        role.getDescription()
                )).collect(Collectors.toSet());
    }

    private UserProfileDto toProfileDto(UserProfile p) {
        if (p == null) return null;
        return new UserProfileDto(
                p.getUserId(),
                p.getAvatarUrl(),
                p.getStreamKey(),
                p.getMessageForSupporters(),
                p.getBio(),
                p.getSocialLinks(),
                p.getMeta()
        );
    }
}
