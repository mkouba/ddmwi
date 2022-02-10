package com.github.mkouba.ddmwi;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.persistence.PersistenceException;

import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.Warband.PointLimit;
import com.github.mkouba.ddmwi.ctrl.HibernateReactivePanache;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class WarbandTest {

    @Test
    public void testRestrictions() {
        Creature base = CreatureTest.creature("base").good().cost(50).civilization().wild().done();
        Creature pointLimit = CreatureTest.creature("pointLimit").cost(150).wild().done();
        Creature invalidFaction = CreatureTest.creature("invalidFaction").underdark().done();
        Creature wrongAlignment = CreatureTest.creature("wrongAlignment").evil().civilization().done();

        Warband quick = new Warband();
        quick.pointLimit = PointLimit.QUICK;
        quick.addCreature(base);

        assertFalse(quick.isPointsLimitOk(pointLimit));
        assertFalse(quick.factionsMatch(invalidFaction.factions));
        assertFalse(quick.isAlignmentOk(wrongAlignment));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> quick.addCreature(pointLimit))
                .withMessage(
                        "Point limit 100 exceeded - remaining points: 50");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> quick.addCreature(invalidFaction))
                .withMessage(
                        "Creature [invalidFaction] factions [UNDERDARK] do not match the warband base factions: [CIVILIZATION, WILD]");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> quick.addCreature(wrongAlignment))
                .withMessage(
                        "Creature alignment [EVIL] does not match the warband alignment: GOOD");
    }

    @RunOnVertxContext
    @Test
    public void testPersist(UniAsserter asserter) {
        Creature c1 = CreatureTest.creature("1").good().cost(50).civilization().wild().done();
        Creature c2 = CreatureTest.creature("2").cost(50).civilization().done();

        asserter
                .execute(() -> {
                    return TestData.perform(
                            () -> User.<User> find("username", "foo").firstResult().call(foo -> Creature.persist(c1, c2))
                                    .chain(
                                            user -> {
                                                Warband w = create(user, "Test").addCreature(c1).addCreature(c2);
                                                return w.persist();
                                            }));
                })
                .assertThat(() -> Warband.<Warband> find("name", "Test").firstResult(), w -> {
                    assertNotNull(w.id);
                    assertEquals(2, w.creatures.size());
                    assertFalse(w.arena);
                })
                // Test unique constraint
                .assertFailedWith(() -> {
                    return TestData.perform(
                            () -> User.<User> find("username", "foo").firstResult()
                                    .chain(
                                            user -> create(user, "Test").persist()));
                }, PersistenceException.class)
                // Always destroy the current session
                .execute(HibernateReactivePanache::destroySession);
    }

    public static Warband create(User user, String name) {
        Warband warband = new Warband();
        warband.user = user;
        warband.name = name;
        return warband;
    }

    public static Uni<Void> deleteAll() {
        // Warband.deleteAll() does not perform cascading operations!
        return HibernateReactivePanache.withTransaction(() -> Warband.<Warband> findAll().list().chain(all -> {
            Uni<Void> uni = Uni.createFrom().voidItem();
            for (Warband w : all) {
                uni = uni.chain(ignored -> w.delete());
            }
            return uni;
        }));
    }

}
