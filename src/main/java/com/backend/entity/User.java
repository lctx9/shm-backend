package com.backend.entity;

import com.backend.entity.enums.StudentType;
import com.backend.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    // Student info
    @Enumerated(EnumType.STRING)
    @Column(name = "student_type", length = 20)
    private StudentType studentType;

    @Column(name = "student_id", length = 50)
    private String studentId;

    @Column(name = "university_name", length = 255)
    private String universityName;

    // Judge info
    @Column(name = "is_guest_judge", nullable = false)
    @Builder.Default
    private boolean isGuestJudge = false;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    // Roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}