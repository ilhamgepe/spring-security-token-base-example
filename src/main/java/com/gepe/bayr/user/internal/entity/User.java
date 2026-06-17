package com.gepe.bayr.user.internal.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gepe.bayr.shared.jpa.BaseAuditEntity;
import com.gepe.bayr.user.api.type.KycStatus;
import com.gepe.bayr.user.api.type.UserStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseAuditEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus = KycStatus.UNVERIFIED;

    @Column(name = "email", nullable = false, columnDefinition = "citext", unique = true)
    String email;

    @Column(name = "nickname", nullable = false, columnDefinition = "citext", unique = true)
    String nickname;


    // relation
    @OneToOne(
        mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
//    @JsonManagedReference // biar ga infinite recursion si jackson
    private UserProfile userProfile;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }

    // Helper method untuk handle @MapsId dari sisi UserProfile
    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            userProfile.setUser(this); // Mengisi null association yang bikin error kemarin
        }
    }

    // Helper method untuk menambah satu Role secara aman
    public void addRole(Role role) {
        this.roles.add(role);
    }

    // Helper method jika ingin menambah banyak Role sekaligus (Bulk)
    public void addRoles(Collection<Role> roles) {
        this.roles.addAll(roles);
    }

}
