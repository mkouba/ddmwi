package com.github.mkouba.ddmwi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class WarbandUnitTest {

    @Test
    public void testPosition() {
        Creature alpha = CreatureTest.creature("a").id(1l).good().cost(50).civilization().wild().build();
        Creature bravo = CreatureTest.creature("b").id(2l).cost(150).wild().build();
        Creature charlie = CreatureTest.creature("c").id(3l).underdark().build();

        Warband warband = new Warband();

        WarbandCreature a = new WarbandCreature();
        a.id = 1l;
        a.warband = warband;
        a.creature = alpha;

        WarbandCreature b = new WarbandCreature();
        b.id = 2l;
        b.warband = warband;
        b.creature = bravo;

        WarbandCreature c = new WarbandCreature();
        c.id = 3l;
        c.warband = warband;
        c.creature = charlie;

        List<WarbandCreature> creatures = new ArrayList<>();
        creatures.add(a);
        creatures.add(b);
        creatures.add(c);
        warband.creatures = creatures;

        warband.ensurePositionsInitialized();

        assertEquals(0, a.position);
        assertEquals(1, b.position);
        assertEquals(2, c.position);

        warband.moveLeft(3);
        assertEquals(warband.creatures.get(1).id, 3l);
        assertEquals(warband.creatures.get(0).id, 1l);
        assertEquals(warband.creatures.get(2).id, 2l);
        assertEquals(warband.creatures.get(1).position, 1);

        warband.moveLeft(3);
        assertEquals(warband.creatures.get(0).id, 3l);
        assertEquals(warband.creatures.get(1).id, 1l);
        assertEquals(warband.creatures.get(2).id, 2l);
        assertEquals(warband.creatures.get(0).position, 0);

        warband.moveRight(1);
        assertEquals(warband.creatures.get(0).id, 3l);
        assertEquals(warband.creatures.get(1).id, 2l);
        assertEquals(warband.creatures.get(2).id, 1l);
    }

}
