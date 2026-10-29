package com.upc.pre.peaceapp.models;

import com.upc.pre.peaceapp.shared.documentation.models.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserProfile extends AuditableAbstractAggregateRoot<UserProfile> {
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    private String lastname;
    private String phonenumber;
    /** Id de la cuenta IAM a la que pertenece el perfil (1 a 1). */
    @Column(unique = true, nullable = false)
    private Long user_id;
    private String profile_image;

    public UserProfile() {
    }

    public UserProfile(String name, String email, String lastname, String phonenumber, Long user_id) {
        this.name = name;
        this.email = email;
        this.lastname = lastname;
        this.phonenumber = phonenumber == null ? "" : phonenumber;
        this.user_id = user_id;
        this.profile_image = "";
    }
}
