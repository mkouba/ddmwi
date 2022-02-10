package com.github.mkouba.ddmwi.ctrl;

import java.util.Set;

import org.jboss.resteasy.reactive.RestForm;

import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.User.Role;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;

public class UserForm extends Form<User> {

    @RestForm
    public String username;

    @RestForm
    public String password;

    @RestForm
    public Set<Role> roles;

    @RestForm
    public boolean active;

    @Override
    protected Uni<User> apply(User user) {
        user.username = username;
        if (password != null && !password.isEmpty()) {
            user.password = BcryptUtil.bcryptHash(password);
        }
        user.roles = roles;
        user.active = user.isAdmin() ? true : active;
        return Uni.createFrom().item(user);
    }

}
