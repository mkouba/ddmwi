package com.github.mkouba.ddmwi.ctrl;

import io.smallrye.mutiny.Uni;

public abstract class Form<T> {

    Uni<T> applyTo(T entity, boolean validate) {
        if (validate) {
            return validateForm(entity).call(this::apply);
        }
        return apply(entity);
    }

    Uni<T> applyTo(T entity) {
        return applyTo(entity, true);
    }
    
    protected Uni<T> apply(T entity) {
        return Uni.createFrom().item(entity);
    }
    
    protected Uni<T> validateForm(T entity) {
        return Uni.createFrom().item(entity);
    }
    
    @SuppressWarnings("serial")
    public static class FormException extends RuntimeException {

        public FormException(String message) {
            super(message);
        }
        
        public String getHtmlMessage() {
            return getMessage().replace("\n", "<br>");
        }

    }
    
}
