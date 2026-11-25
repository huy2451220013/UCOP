package com.ucop.entity;

import com.ucop.enums.UserRole;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, unique = true, length = 20)
    private UserRole roleName;

    @Column(name = "description", length = 255)
    private String description;

    public Role() {}

    public Role(UserRole roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserRole getRoleName() { return roleName; }
    public void setRoleName(UserRole roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return roleName == role.roleName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleName);
    }
}
