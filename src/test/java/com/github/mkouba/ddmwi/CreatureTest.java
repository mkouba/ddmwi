package com.github.mkouba.ddmwi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.function.Consumer;

import javax.persistence.PersistenceException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;
import com.github.mkouba.ddmwi.ctrl.HibernateReactivePanache;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
public class CreatureTest {

    @RunOnVertxContext
    @Test
    public void testPersist(UniAsserter asserter) {
        asserter.execute(() -> TestData.performPersist(creature("Angel").good().cost(50).civilization().wild().done()))
                .assertThat(() -> Creature.<Creature> find("name", "Angel").firstResult(), c -> {
                    assertNotNull(c);
                    assertNotNull(c.id);
                })
                // Test unique constraint
                .assertFailedWith(() -> TestData.performPersist(creature("Angel").evil().cost(150).civilization().done()),
                        PersistenceException.class)
                // Always destroy the current session
                .execute(HibernateReactivePanache::destroySession);
    }

    @AfterAll
    static void delete() {
        TestData.perform(() -> Creature.deleteAll()).await().atMost(Duration.ofSeconds(5));
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

        public Creature done() {
            return creature;
        }
    }

}
