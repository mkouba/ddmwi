package com.github.mkouba.ddmwi.security;

import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;

import org.hibernate.reactive.mutiny.Mutiny.Session;
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.util.ModularCrypt;

import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.User.Role;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class UserIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    public static final String USER_ID = "userId";

    private static final Logger LOG = Logger.getLogger(UserIdentityProvider.class);

    // For some reason we can't use Panache inside IdentityProvider.authenticate()
    @Inject
    SessionFactory factory;

    @Inject
    UserActivityTracker activityTracker;
    
    @Inject
    Event<UserLoggedIn> userLoggedIn;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return factory.withTransaction(
                (s, t) -> findUser(s, request.getUsername()).chain(u -> authenticateUser(request, u)));
    }

    private Uni<User> findUser(Session session, String username) {
        return session.<User> createQuery("from User where username = :username").setParameter("username", username)
                .getResultList().chain(users -> {
                    if (users.isEmpty() || users.size() > 1) {
                        throw new AuthenticationFailedException();
                    }
                    return Uni.createFrom().item(users.get(0));
                });
    }

    private Uni<SecurityIdentity> authenticateUser(UsernamePasswordAuthenticationRequest request, User user) {
        if (!user.active) {
            LOG.warnf("User not active: %s", user.username);
            throw new AuthenticationFailedException("User not active");
        }
        PasswordGuessEvidence evidence = new PasswordGuessEvidence(request.getPassword().getPassword());
        PasswordCredential credential = new PasswordCredential(getMcfPassword(user.password));
        if (!credential.verify(Security::getProviders, evidence)) {
            LOG.warnf("Invalid credentials: %s", user.username);
            throw new AuthenticationFailedException("Invalid credentials");
        }
        LOG.infof("User authenticated: %s", request.getUsername());
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(request.getUsername()));
        builder.addCredential(request.getPassword());
        builder.addAttribute(USER_ID, user.id);
        addRoles(user, builder);

        user.lastLogin = LocalDateTime.now();
        activityTracker.update(user.username);
        userLoggedIn.fire(new UserLoggedIn(user.username));
        
        return Uni.createFrom().item(builder.build());
    }

    static Password getMcfPassword(String pass) {
        try {
            return ModularCrypt.decode(pass);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    static void addRoles(User user, QuarkusSecurityIdentity.Builder builder) {
        for (Role role : user.roles) {
            builder.addRole(role.strValue());
        }
    }
    
    
    public record UserLoggedIn(String username) {
    }


}
