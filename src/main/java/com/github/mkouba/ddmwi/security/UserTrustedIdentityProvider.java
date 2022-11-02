package com.github.mkouba.ddmwi.security;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny.StatelessSession;
import org.jboss.logging.Logger;

import com.github.mkouba.ddmwi.User;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@Singleton
public class UserTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    private static final Logger LOG = Logger.getLogger(UserTrustedIdentityProvider.class);

    // For some reason we can't use Panache inside IdentityProvider.authenticate()
    @Inject
    SessionFactory factory;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request, AuthenticationRequestContext context) {
        return factory.openStatelessSession()
                .chain(session -> findUser(session, request.getPrincipal()).eventually(session::close))
                .chain(u -> authenticateUser(request, u));
    }

    private Uni<User> findUser(StatelessSession session, String username) {
        return session.<User> createQuery("from User where username = :username").setParameter("username", username)
                .getResultList().chain(users -> {
                    if (users.isEmpty() || users.size() > 1) {
                        throw new AuthenticationFailedException("User not found");
                    }
                    return Uni.createFrom().item(users.get(0));
                });
    }

    private Uni<SecurityIdentity> authenticateUser(TrustedAuthenticationRequest request, User user) {
        if (!user.active) {
            return Uni.createFrom().failure(new AuthenticationFailedException("User is not active"));
        }
        LOG.debugf("User authenticated [trusted]: %s", request.getPrincipal());
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(request.getPrincipal()));
        builder.addAttribute("userId", user.id);
        UserIdentityProvider.addRoles(user, builder);

        return Uni.createFrom().item(builder.build());
    }

}
