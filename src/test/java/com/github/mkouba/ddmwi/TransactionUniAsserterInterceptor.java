package com.github.mkouba.ddmwi;

import java.util.function.Supplier;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class TransactionUniAsserterInterceptor extends UniAsserterInterceptor {

    public TransactionUniAsserterInterceptor(UniAsserter asserter) {
        super(asserter);
    }

    @Override
    protected <T> Supplier<Uni<T>> aroundUni(Supplier<Uni<T>> uniSupplier) {
        return () -> Panache.withTransaction(uniSupplier);
    }

}