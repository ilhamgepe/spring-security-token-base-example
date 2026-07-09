package com.gepe.bayr.user.internal.repo;

import com.gepe.bayr.user.internal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    @Query("""
        SELECT u FROM User u
        JOIN FETCH u.userProfile
        JOIN FETCH u.roles
        WHERE u.id = :userId
    """)
    Optional<User> findByIdWithDetails(@Param("userId") UUID userId);
}
