package com.github.mkouba.ddmwi;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.qute.TemplateEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(name = "user_username", columnNames = "username"))
public class User extends PanacheEntity {

    public boolean active;

    @NotEmpty
    @Size(max = 20)
    public String username;

    @NotEmpty
    @Size(max = 100)
    public String password;

    @NotEmpty
    @Convert(converter = RolesConverter.class)
    @Column(name = "roles", length = 50)
    public Set<Role> roles = EnumSet.noneOf(Role.class);

    public LocalDateTime lastLogin;

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    @TemplateEnum
    public enum Role {
        USER(Role.USER_STR),
        ADMIN(Role.ADMIN_STR);

        private final String str;

        Role(String str) {
            this.str = str;
        }

        public String strValue() {
            return str;
        }

        public static final String USER_STR = "user";
        public static final String ADMIN_STR = "admin";

    }

}
