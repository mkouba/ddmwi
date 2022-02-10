package com.github.mkouba.ddmwi;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.qute.TemplateEnum;

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
