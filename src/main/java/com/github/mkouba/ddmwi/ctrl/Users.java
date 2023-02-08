package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;
import java.util.Collections;

import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.User;
import com.github.mkouba.ddmwi.UserCreature;
import com.github.mkouba.ddmwi.Warband;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_HTML)
@Path(Users.PATH)
public class Users extends Controller {

    static final String PATH = "/users";

    @GET
    public Uni<TemplateInstance> list() {
        return User.<User> listAll().map(u -> Templates.users(u));
    }

    @GET
    @Path("new")
    public Uni<TemplateInstance> newUser() {
        return toUni(Templates.user(new User(), Collections.emptyList()));
    }

    @WithTransaction
    @POST
    @Path("new")
    public Uni<RestResponse<Object>> create(@BeanParam UserForm form) {
        URI listUri = uriFrom(PATH);
        Uni<User> user = form.apply(new User()).chain(u -> u.persistAndFlush());
        return user.onItem().transform(v -> RestResponse.seeOther(listUri))
                .onFailure()
                .recoverWithUni(t -> {
                    return recoverWithNewSession(() -> failureToResponse(t,
                            messages -> toUni(Templates.user(new User(), messages)),
                            cause -> Qute.fmt("User with name \"{}\" already exists", form.username)));
                });
    }

    @GET
    @Path("{id}")
    public Uni<TemplateInstance> get(Long id) {
        return User.<User> findById(id).map(u -> Templates.user(u, Collections.emptyList()));
    }

    @POST
    @Path("{id}")
    public Uni<RestResponse<Object>> update(Long id, @BeanParam UserForm form) {
        URI listUri = uriFrom(PATH);
        return Panache.withTransaction(() -> User.<User> findById(id)
                .onItem().ifNotNull().call(form::applyTo))
                .onItem().ifNotNull().transform(v -> RestResponse.seeOther(listUri))
                .onItem().ifNull().continueWith(RestResponse.notFound())
                .onFailure().recoverWithUni(t -> {
                    return recoverWithNewSession(() -> failureToResponse(t,
                            messages -> User.<User> findById(id).map(u -> Templates.user(u, messages)),
                            cause -> Qute.fmt("User with name \"{}\" already exists", form.username)));

                });
    }

    @WithTransaction
    @POST
    @Path("{id}/delete")
    public Uni<RestResponse<Object>> delete(Long id) {
        URI listUri = uriFrom(PATH);
        // Delete collection, warbands and user
        return Warband.delete("user.id", id).chain(
                dw -> UserCreature.delete("user.id", id).chain(
                        dc -> User.deleteById(id).map(
                                du -> RestResponse.seeOther(listUri))));
    }

}
