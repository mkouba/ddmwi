package com.github.mkouba.ddmwi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Consumer;

import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;
import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;

import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;

@QuarkusTest
public class CreatureTest {

    @Inject
    SessionFactory sessionFactory;

    @RunOnVertxContext
    @Test
    public void testPersist(TransactionalUniAsserter asserter) {
        asserter.execute(() -> Creature.persist(creature("Angel").good().cost(50).civilization().wild().build()))
                .assertThat(() -> Creature.<Creature> find("name", "Angel").firstResult(), c -> {
                    assertNotNull(c);
                    assertNotNull(c.id);
                })
                .execute(() -> Creature.deleteAll());
    }

    @RunOnVertxContext
    @Test
    public void testUniqueConstraint(UniAsserter asserter) {
        asserter = new TransactionUniAsserterInterceptor(asserter);
        asserter.execute(() -> Creature.persist(creature("Angel").good().cost(50).civilization().wild().build()))
                .assertFailedWith(() -> Creature.persist(creature("Angel").evil().cost(150).civilization().build()),
                        PersistenceException.class)
                .execute(() -> Creature.deleteAll());
    }

    public static CreatureBuilder creature(String name) {
        return new CreatureBuilder(name);
    }

    public static class CreatureBuilder {

        private Creature creature = new Creature();

        CreatureBuilder(String name) {
            creature.name = name;
        }

        public CreatureBuilder good() {
            creature.alignment = Alignment.GOOD;
            return this;
        }

        public CreatureBuilder evil() {
            creature.alignment = Alignment.EVIL;
            return this;
        }

        public CreatureBuilder cost(int cost) {
            creature.cost = cost;
            return this;
        }

        public CreatureBuilder borderlands() {
            creature.factions.add(Faction.BORDERLANDS);
            return this;
        }

        public CreatureBuilder civilization() {
            creature.factions.add(Faction.CIVILIZATION);
            return this;
        }

        public CreatureBuilder underdark() {
            creature.factions.add(Faction.UNDERDARK);
            return this;
        }

        public CreatureBuilder wild() {
            creature.factions.add(Faction.WILD);
            return this;
        }

        public CreatureBuilder attr(Consumer<Creature> consumer) {
            consumer.accept(creature);
            return this;
        }

        public Creature build() {
            return creature;
        }
    }
}
