package com.gepe.bayr.user.internal.service;

import com.gepe.bayr.shared.exception.ErrorMessage;
import com.gepe.bayr.shared.exception.ServiceException;
import com.gepe.bayr.user.api.UserFacade;
import com.gepe.bayr.user.api.dto.RoleRes;
import com.gepe.bayr.user.api.dto.UserProfileRes;
import com.gepe.bayr.user.api.dto.UserRes;
import com.gepe.bayr.user.api.type.UserStatus;
import com.gepe.bayr.user.internal.entity.Role;
import com.gepe.bayr.user.internal.entity.User;
import com.gepe.bayr.user.internal.entity.UserProfile;
import com.gepe.bayr.user.internal.repo.RoleRepo;
import com.gepe.bayr.user.internal.repo.UserRepo;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserFacadeImpl implements UserFacade {
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
    public UserRes getById(UUID id) {
        return userRepo.findById(id).map(
                user -> new UserRes(
                        user.getId(), user.getEmail(),
                        user.getNickname(),user.getStatus(),user.getKycStatus(), toProfileRes(user.getUserProfile()), toRoleRes(user.getRoles())
                )).orElseThrow(() -> new ServiceException(ErrorMessage.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public UserRes getByIdDetails(UUID id) {
        return userRepo.findByIdWithDetails(id).map(this::toUserRes)
                .orElseThrow(() -> new ServiceException(ErrorMessage.USER_NOT_FOUND));
    }

    /**
     * register user, create user, user profile, dan assign role 'USER'
     * @param email
     * @param nickname
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRes registerUser(String email, String nickname) {
        if (userRepo.existsByEmail(email)) {
            throw new ServiceException(ErrorMessage.USER_EMAIL_ALREADY_EXISTS);
        }
        if (userRepo.existsByNickname(nickname)) {
            throw new ServiceException(ErrorMessage.USER_NICKNAME_ALREADY_EXISTS);
        }
        UUID id = UuidCreator.getTimeOrderedEpoch();
        String streamKey = generateStreamKey();

        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());


        // register user profile
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(user.getId());
        userProfile.setStreamKey(streamKey);
        userProfile.setMessageForSupporters("Thank you for your support!");

        user.setUserProfile(userProfile);

        // set role
        Role defaultRole = roleRepo.findByCode("USER")
                .orElseThrow(() -> new ServiceException(ErrorMessage.ROLE_NOT_FOUND));


        user.addRole(defaultRole);

        // save user and user profile
        userRepo.save(user);


        return toUserRes(user);
    }

    private String generateStreamKey() {
        UUID uuid = UuidCreator.getTimeOrderedEpoch();
        return "live_"+ uuid.toString().replace("-", "");
    }

    private UserRes toUserRes(User user) {
        return new UserRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getKycStatus(),
                toProfileRes(user.getUserProfile()),
                toRoleRes(user.getRoles())
        );
    }

    private Set<RoleRes> toRoleRes(Set<Role> roles) {
        return roles.stream()
                .map(role -> new RoleRes(
                        role.getId(),
                        role.getCode(),
                        role.getName(),
                        role.getDescription()
                )).collect(Collectors.toSet());
    }

    private UserProfileRes toProfileRes(UserProfile p) {
        if (p == null) return null;
        return new UserProfileRes(
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
