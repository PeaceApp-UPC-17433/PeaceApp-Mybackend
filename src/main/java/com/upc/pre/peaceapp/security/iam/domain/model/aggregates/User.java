package com.upc.pre.peaceapp.security.iam.domain.model.aggregates;

import com.upc.pre.peaceapp.security.iam.domain.model.entities.Role;
import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles;
import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "iam_users")
public class User extends AuditableAbstractAggregateRoot<User> {
    @Setter
    @NotBlank
    @Getter
    @Size(max = 50)
    @Column(unique = true)
    private String username;

    @Setter
    @Getter
    @NotBlank
    @Size(max = 120)
    private String password;


    @Setter
    @Getter
    @ManyToMany(fetch = FetchType.EAGER) // sin cascade
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    public User() {
        this.roles = new HashSet<>();

    }
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.roles = new HashSet<>();

    }

    public User(String username, String password, List<Role> roles) {
        this(username, password);
        addRoles(roles);

    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeUsername(String username) {
        this.username = username;
    }

    public User addRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public User addRoles(List<Role> roles) {
        //var validatedRoles = Role.validateRoleSet(roles);
        this.roles.addAll(roles);
        return this;
    }

    public boolean hasRole(Roles role) {
        return roles.stream().anyMatch(r -> r.getName() == role);
    }

    /** Reemplaza los roles actuales por los indicados. */
    public User replaceRoles(List<Role> newRoles) {
        this.roles.clear();
        this.roles.addAll(newRoles);
        return this;
    }
}
