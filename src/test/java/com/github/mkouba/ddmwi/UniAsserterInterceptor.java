package com.github.mkouba.ddmwi;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public abstract class UniAsserterInterceptor implements UniAsserter {

    private final UniAsserter delegate;

    public UniAsserterInterceptor(UniAsserter asserter) {
        this.delegate = asserter;
    }

    protected <T> Supplier<Uni<T>> aroundUni(Supplier<Uni<T>> uniSupplier) {
        return uniSupplier;
    }
    
    protected UniAsserter aroundExecute(Runnable c) {
        delegate.execute(c);
        return this;
    }
    
    protected UniAsserter aroundFail() {
        delegate.fail();
        return this;
    }

    @Override
    public <T> UniAsserter assertThat(Supplier<Uni<T>> uni, Consumer<T> asserter) {
        delegate.assertThat(aroundUni(uni), asserter);
        return this;
    }

    @Override
    public <T> UniAsserter execute(Supplier<Uni<T>> uni) {
        delegate.execute(aroundUni(uni));
        return this;
    }

    @Override
    public UniAsserter execute(Runnable c) {
        return aroundExecute(c);
    }

    @Override
    public <T> UniAsserter assertEquals(Supplier<Uni<T>> uni, T t) {
        delegate.assertEquals(aroundUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertNotEquals(Supplier<Uni<T>> uni, T t) {
        delegate.assertNotEquals(aroundUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertSame(Supplier<Uni<T>> uni, T t) {
        delegate.assertSame(aroundUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertNotSame(Supplier<Uni<T>> uni, T t) {
        delegate.assertNotSame(aroundUni(uni), t);
        return this;
    }

    @Override
    public <T> UniAsserter assertNull(Supplier<Uni<T>> uni) {
        delegate.assertNull(aroundUni(uni));
        return this;
    }

    @Override
    public <T> UniAsserter assertNotNull(Supplier<Uni<T>> uni) {
        delegate.assertNotNull(aroundUni(uni));
        return this;
    }

    @Override
    public <T> UniAsserter surroundWith(Function<Uni<T>, Uni<T>> uni) {
        delegate.surroundWith(uni);
        return this;
    }

    @Override
    public UniAsserter fail() {
        return aroundFail();
    }

    @Override
    public UniAsserter assertTrue(Supplier<Uni<Boolean>> uni) {
        delegate.assertTrue(aroundUni(uni));
        return this;
    }

    @Override
    public UniAsserter assertFalse(Supplier<Uni<Boolean>> uni) {
        delegate.assertFalse(aroundUni(uni));
        return this;
    }

    @Override
    public <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Consumer<Throwable> c) {
        delegate.assertFailedWith(aroundUni(uni), c);
        return this;
    }

    @Override
    public <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Class<? extends Throwable> c) {
        delegate.assertFailedWith(aroundUni(uni), c);
        return this;
    }

    @Override
    public Object getData(String key) {
        return delegate.getData(key);
    }

    @Override
    public Object putData(String key, Object value) {
        return delegate.putData(key, value);
    }

    @Override
    public void clearData() {
        delegate.clearData();
    }

}