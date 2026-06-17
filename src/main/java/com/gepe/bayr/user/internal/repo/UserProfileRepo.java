package com.gepe.bayr.user.internal.repo;

import com.gepe.bayr.user.internal.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserProfileRepo extends JpaRepository<UserProfile, UUID> {
}
