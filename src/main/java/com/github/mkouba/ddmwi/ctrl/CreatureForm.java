package com.github.mkouba.ddmwi.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.RestForm;

import com.github.mkouba.ddmwi.Creature;
import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;
import com.github.mkouba.ddmwi.Creature.MovementMode;
import com.github.mkouba.ddmwi.Warband;
import com.github.mkouba.ddmwi.dao.WarbandDao;

import io.quarkus.arc.Arc;
import io.quarkus.qute.Qute;
import io.smallrye.mutiny.Uni;

public class CreatureForm extends Form<Creature> {

    @RestForm
    public String name;

    @RestForm
    public int cost;

    @RestForm
    public int level;

    @RestForm
    public String setInfo;

    @RestForm
    public int hp;

    @RestForm
    public int speed;

    @RestForm
    public MovementMode movementMode;

    @RestForm
    public int ac;

    @RestForm
    public int ref;

    @RestForm
    public int fort;

    @RestForm
    public int will;

    @RestForm
    public Alignment alignment;

    @RestForm
    public Set<Faction> factions;

    @RestForm
    public String keywords;

    @RestForm
    public int champ;

    protected Uni<Creature> apply(Creature creature) {
        creature.name = name;
        creature.setInfo = setInfo;
        creature.level = level;
        creature.hp = hp;
        creature.speed = speed;
        creature.movementMode = movementMode;
        creature.alignment = alignment;
        creature.cost = cost;
        creature.factions = factions;
        creature.ac = ac;
        creature.fort = fort;
        creature.ref = ref;
        creature.will = will;
        creature.keywords = keywords;
        creature.championRating = champ;
        return Uni.createFrom().item(creature);
    }

    protected Uni<Creature> validateForm(Creature creature) {
        // If we increase the cost we should validate limits in all warbands that contain the creature
        WarbandDao warbandDao = Arc.container().instance(WarbandDao.class).get();
        return warbandDao.findAllWarbands().invoke(warbands -> {
            Set<Warband> affected = warbands.stream().filter(w -> !w.freestyle && w.containsCreature(creature))
                    .collect(Collectors.toSet());
            List<String> errors = new ArrayList<>();
            for (Warband w : affected) {
                if (w.getRemainingPoints() < (cost - creature.cost)) {
                    errors.add(Qute.fmt("Point limit [{}] exceeded in warband {}", w.pointLimit.value, w.name));
                }
                if (alignment != Alignment.NEUTRAL) {
                    Alignment warbandAlignment = w.getAlignment(c -> c.equals(creature));
                    if (warbandAlignment != Alignment.NEUTRAL
                            && alignment != warbandAlignment) {
                        errors.add(
                                Qute.fmt("Selected alignment conflicts with [{}] in warband {}", warbandAlignment, w.name));
                    }
                }
                // FIXME ignore the creature factions!!! 
                if (!w.factionsMatch(factions)) {
                    errors.add(
                            Qute.fmt("Selected factions do not match {} in warband {}", w.getBaseFactions(), w.name));
                }
            }
            if (!errors.isEmpty()) {
                throw new FormException(
                        "Creature cannot be modified:\n\t- " + errors.stream().collect(Collectors.joining("\n\t- ")));
            }
        }).map(warbands -> creature);
    }

}
