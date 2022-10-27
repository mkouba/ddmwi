package com.github.mkouba.ddmwi.ctrl;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

import com.github.mkouba.ddmwi.ctrl.Form.FormException;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.util.ExceptionUtil;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerResponse;

public abstract class Controller {

    private static final Logger LOG = Logger.getLogger(Controller.class);

    @Inject
    UriInfo uriInfo;

    @Inject
    HttpServerResponse response;

    @Inject
    Validator validator;

    protected <T> Uni<RestResponse<Object>> failureToResponse(Throwable t, Function<List<String>, TemplateInstance> templateFun,
            Function<Throwable, String> uniqueConstraintViloated) {
        Throwable cause = ExceptionUtil.getRootCause(t);
        if (cause instanceof ConstraintViolationException) {
            TemplateInstance template = templateFun.apply(createViolationsMessages(cause));
            return template.createUni().map(s -> RestResponse.ok(s, MediaType.TEXT_HTML_TYPE));
        } else if (cause instanceof FormException) {
            TemplateInstance template = templateFun.apply(List.of(((FormException) cause).getHtmlMessage()));
            return template.createUni().map(s -> RestResponse.ok(s, MediaType.TEXT_HTML_TYPE));
        } else if (uniqueNameConstraintViloated(cause.getMessage())) {
            TemplateInstance template = templateFun.apply(List.of(uniqueConstraintViloated.apply(t)));
            return template.createUni().map(s -> RestResponse.ok(s, MediaType.TEXT_HTML_TYPE));
        } else {
            LOG.error("Internal server error: ", t);
            return Templates.error().createUni().map(s -> RestResponse.ok(s, MediaType.TEXT_HTML_TYPE));
        }
    }

    static boolean uniqueNameConstraintViloated(String message) {
        return message.contains("duplicate key value violates unique constraint");
    }

    <T> List<String> validate(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (violations.isEmpty()) {
            return Collections.emptyList();
        }
        return createMessages(cast(violations));
    }

    List<String> createViolationsMessages(Throwable t) {
        Set<ConstraintViolation<?>> violations = ((ConstraintViolationException) t)
                .getConstraintViolations();
        return createMessages(violations);
    }

    List<String> createMessages(Set<ConstraintViolation<?>> violations) {
        List<String> messages = new ArrayList<>();
        for (ConstraintViolation<?> violation : violations) {
            messages.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }
        return messages;
    }

    protected void setHtmxPush(String url, Object... args) {
        response.putHeader("HX-Push", String.format(url, Arrays.stream(args).map(Controller::encode).toArray()));
    }
    
    protected URI uriFrom(String path) {
        return uriInfo.getRequestUriBuilder().replacePath(path).build();
    }
    
    protected URI uriFrom(String path, String query) {
        return uriInfo.getRequestUriBuilder().replacePath(path).replaceQuery(query).build();
    }


    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj) {
        return (T) obj;
    }

    static String encode(Object value) {
        return encode(value.toString());
    }

    static String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
