package com.github.mkouba.ddmwi.ctrl;

import java.time.Duration;

import org.jboss.resteasy.reactive.RestStreamElementType;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.security.UserActivityTracker;
import com.github.mkouba.ddmwi.security.UserActivityTracker.UserRemoved;
import com.github.mkouba.ddmwi.security.UserIdentityProvider.UserLoggedIn;

import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;

@Path(Dashboard.PATH)
public class Dashboard extends Controller {

    static final String PATH = "/dashboard";

    @Inject
    UserActivityTracker activityTracker;

    @Inject
    Sse sse;

    private final BroadcastProcessor<OutboundSseEvent> activeUsers = BroadcastProcessor.create();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> get() {
        return User.count()
                .chain(uc -> Creature.count()
                        .chain(cc -> Warband.count()
                                .chain(wc -> toUni(
                                        Templates.dashboard(new Info(uc, cc, wc), activityTracker.getLastActivity())))));
    }

    @Path("active-users")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> activeUsers() {
        return toUni(Templates.dashboard$activeUsers(activityTracker.getLastActivity()));
    }

    @Path("sse-active-users")
    @GET
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<OutboundSseEvent> sseActiveUsers() {
        return Multi.createBy().merging().streams(activeUsers,
                Multi.createFrom()
                        .ticks()
                        .every(Duration.ofSeconds(10))
                        // Send an empty message every ten seconds to workaround problems with cloud providers that can close the connection after 10s of inactivity
                        .onItem().transform(t -> sse.newEvent("ping", "")));
    }

    void onUserRemoved(@Observes UserRemoved user) {
        activeUsers.onNext(sse.newEventBuilder().name("change").data(user.username()).build());
    }

    void onUserLoggedIn(@Observes UserLoggedIn user) {
        activeUsers.onNext(sse.newEventBuilder().name("change").data(user.username()).build());
    }

    public record Info(long users, long creatures, long warbands) {
    }
}
