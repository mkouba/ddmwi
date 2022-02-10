package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;
import java.util.Collections;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.UserCreature;
import com.github.mkouba.ddmwi.Warband;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

@Produces(MediaType.TEXT_HTML)
@NonBlocking
@Path("/users")
public class Users extends Controller {

    @GET
    public TemplateInstance list() {
        return Templates.users(User.listAll());
    }

    @GET
    @Path("new")
    public TemplateInstance newUser() {
        return Templates.user(Uni.createFrom().item(new User()), Collections.emptyList());
    }

    @POST
    @Path("new")
    public Uni<RestResponse<Object>> create(@BeanParam UserForm form) {
        URI listUri = uriInfo.getRequestUriBuilder().replacePath("/users").build();
        Uni<User> user = Panache.withTransaction(() -> form.apply(new User()).chain(u -> u.persistAndFlush()));
        return user.onItem().transform(v -> RestResponse.seeOther(listUri))
                .onFailure()
                .recoverWithUni(t -> {
                    HibernateReactivePanache.destroySession();
                    return failureToResponse(t,
                            messages -> Templates.user(Uni.createFrom().item(new User()), messages),
                            cause -> Qute.fmt("User with name \"{}\" already exists", form.username));
                });
    }

    @GET
    @Path("{id}")
    public TemplateInstance get(Long id) {
        return Templates.user(User.<User> findById(id).memoize().indefinitely(), Collections.emptyList());
    }

    @POST
    @Path("{id}")
    public Uni<RestResponse<Object>> update(Long id, @BeanParam UserForm form) {
        URI listUri = uriInfo.getRequestUriBuilder().replacePath("/users").build();
        return Panache.withTransaction(() -> User.<User> findById(id)
                .onItem().ifNotNull().call(form::applyTo))
                .onItem().ifNotNull().transform(v -> RestResponse.seeOther(listUri))
                .onItem().ifNull().continueWith(RestResponse.notFound())
                .onFailure().recoverWithUni(t -> {
                    HibernateReactivePanache.destroySession();
                    return failureToResponse(t,
                            messages -> Templates.user(User.<User> findById(id), messages),
                            cause -> Qute.fmt("User with name \"{}\" already exists", form.username));

                });
    }

    @POST
    @Path("{id}/delete")
    public Uni<RestResponse<Object>> delete(Long id) {
        URI listUri = uriInfo.getRequestUriBuilder().replacePath("/users").build();
        // Delete collection, warbands and user
        return Panache.withTransaction(() -> Warband.delete("user.id", id).chain(
                dw -> UserCreature.delete("user.id", id).chain(
                        dc -> User.deleteById(id).map(
                                du -> RestResponse.seeOther(listUri)))));
    }

}
