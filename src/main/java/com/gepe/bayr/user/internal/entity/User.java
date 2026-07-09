package com.gepe.bayr.user.internal.entity;

import com.gepe.bayr.user.api.type.KycStatusType;
import com.gepe.bayr.user.api.type.UserStatusType;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 100)
    private UserStatusType status = UserStatusType.ACTIVE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 100)
    private KycStatusType kycStatus =  KycStatusType.UNVERIFIED;

    @NotNull
    @Column(name = "email", nullable = false, columnDefinition = "citext", length = Integer.MAX_VALUE)
    private String email;

    @NotNull
    @Column(name = "nickname", nullable = false, columnDefinition = "citext", length = Integer.MAX_VALUE)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    @OneToOne(
        mappedBy = "user",
        fetch = FetchType.EAGER, // ini wajib eager, karna jpa defaultnya eager ketika entity tidak owning side
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private UserProfile userProfile;

    // Helper method untuk handle @MapsId dari sisi UserProfile
    public void setUserProfile(UserProfile userProfile) {
        if (userProfile != null) {
            userProfile.setUser(this); // Mengisi null association yang bikin error kemarin
        }
        this.userProfile = userProfile;

    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Helper method untuk menambah satu Role secara aman
    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
    }

    // Helper method jika ingin menambah banyak Role sekaligus (Bulk)
    public void addRoles(Collection<Role> roles) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.addAll(roles);
    }




}