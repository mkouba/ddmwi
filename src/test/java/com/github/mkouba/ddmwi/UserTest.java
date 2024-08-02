package com.github.mkouba.ddmwi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.User.Role;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;

@QuarkusTest
public class UserTest {

    @RunOnVertxContext
    @Test
    public void testPersist(TransactionalUniAsserter asserter) {
        asserter.execute(() -> create("foo", "foo", Role.USER).persist())
                .assertThat(() -> User.<User> find("username", "foo").firstResult(), u -> {
                    assertNotNull(u);
                    assertNotNull(u.id);
                    assertTrue(u.active);
                    assertFalse(u.isAdmin());
                    assertNull(u.lastLogin);
                }).execute(() -> User.deleteAll());
    }

    public static User create(String username, String password, Role... roles) {
        User user = new User();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.roles = Set.of(roles);
        user.active = true;
        return user;
    }

}
