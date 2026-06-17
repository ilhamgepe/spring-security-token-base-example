package com.gepe.bayr.user.internal.repo;

import com.gepe.bayr.user.internal.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepo extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
}
