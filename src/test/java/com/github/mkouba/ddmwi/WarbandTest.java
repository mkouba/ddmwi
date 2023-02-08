package com.github.mkouba.ddmwi;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

import com.github.mkouba.ddmwi.User.Role;
import com.github.mkouba.ddmwi.Warband.PointLimit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.PersistenceException;

@QuarkusTest
public class WarbandTest {

    @Test
    public void testRestrictions() {
        Creature base = CreatureTest.creature("base").good().cost(50).civilization().wild().build();
        Creature pointLimit = CreatureTest.creature("pointLimit").cost(150).wild().build();
        Creature invalidFaction = CreatureTest.creature("invalidFaction").underdark().build();
        Creature wrongAlignment = CreatureTest.creature("wrongAlignment").evil().civilization().build();

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
        asserter = new TransactionUniAsserterInterceptor(asserter);
        Creature c1 = CreatureTest.creature("1").good().cost(50).civilization().wild().build();
        Creature c2 = CreatureTest.creature("2").cost(50).civilization().build();
        asserter
                // Persist user and creatures
                .execute(() -> persistAll(c1, c2))
                // Create a warband
                .execute(() -> createWarband("Test", c1, c2))
                .assertThat(() -> Warband.<Warband> find("name", "Test").firstResult()
                        .chain(w -> Mutiny.fetch(w.creatures).map(wc -> w)), w -> {
                            assertNotNull(w.id);
                            assertEquals(2, w.creatures.size());
                            assertFalse(w.arena);
                        })
                .execute(this::deleteAll);
    }

    @RunOnVertxContext
    @Test
    public void testUniqueConstraint(UniAsserter asserter) {
        asserter = new TransactionUniAsserterInterceptor(asserter);
        Creature c1 = CreatureTest.creature("1").good().cost(50).civilization().wild().build();
        Creature c2 = CreatureTest.creature("2").cost(50).civilization().build();
        asserter
                // Persist user and creatures
                .execute(() -> persistAll(c1, c2))
                // Create a warband
                .execute(() -> createWarband("Test", c1, c2))
                .assertFailedWith(() -> createWarband("Test"), PersistenceException.class)
                .execute(this::deleteAll);
    }

    private Uni<Warband> createWarband(String name, Creature... creatures) {
        return findFoo()
                .chain(user -> {
                    Warband w = create(user, name);
                    for (Creature creature : creatures) {
                        w.addCreature(creature);
                    }
                    return w.persist();
                });
    }

    private Uni<Void> persistAll(Creature c1, Creature c2) {
        return UserTest.create("foo", "foo", Role.USER).persist().chain(ignored -> Creature.persist(c1, c2));
    }

    private Uni<User> findFoo() {
        return User.<User> find("username", "foo").firstResult();
    }

    public static Warband create(User user, String name) {
        Warband warband = new Warband();
        warband.user = user;
        warband.name = name;
        return warband;
    }

    public Uni<Void> deleteAll() {
        return Warband.<Warband> findAll().list().chain(all -> {
            Uni<Void> uni = Uni.createFrom().voidItem();
            for (Warband w : all) {
                uni = uni.chain(ignored -> w.delete());
            }
            return uni;
        })
                .chain(i -> WarbandCreature.deleteAll())
                .chain(i -> Creature.deleteAll())
                .chain(i -> User.deleteAll()).replaceWithVoid();
    }

}
