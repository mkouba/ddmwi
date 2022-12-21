package com.github.mkouba.ddmwi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;
import com.github.mkouba.ddmwi.Creature.MovementMode;
import com.github.mkouba.ddmwi.CreaturePower.PowerType;

public interface CreatureView {

    Creature creature();

    default Long getId() {
        return getCreatureId();
    }

    default Long getCreatureId() {
        return creature().id;
    }

    default boolean isWarbandCreature() {
        return false;
    }

    default String getName() {
        return creature().name;
    }

    default int getCost() {
        return creature().cost;
    }

    default int getHp() {
        return creature().hp;
    }

    default int getLevel() {
        return creature().level;
    }

    default int getChampionRating() {
        return creature().championRating;
    }

    default Alignment getAlignment() {
        return creature().alignment;
    }

    default int getAc() {
        return creature().ac;
    }

    default int getRef() {
        return creature().ref;
    }

    default int getWill() {
        return creature().will;
    }

    default int getFort() {
        return creature().fort;
    }

    default int getSpeed() {
        return creature().speed;
    }

    default MovementMode getMovementMode() {
        return creature().movementMode;
    }

    default Set<Faction> getFactions() {
        return creature().factions;
    }

    default String getKeywords() {
        return creature().keywords;
    }

    default List<CreaturePower> getPowers() {
        return creature().powers;
    }

    default String getSetInfo() {
        return creature().setInfo;
    }

    default boolean isFlying() {
        return MovementMode.FLIGHT.equals(getMovementMode());
    }

    default boolean isUnique() {
        return getKeywordsList().stream().filter(k -> k.equalsIgnoreCase("unique")).findAny().isPresent();
    }

    default boolean isChampion() {
        return creature().championRating > 0;
    }

    default List<String> getKeywordsList() {
        String keywords = getKeywords();
        return keywords == null || keywords.isEmpty() ? Collections.emptyList()
                : Arrays.stream(keywords.split(",")).map(String::trim).collect(Collectors.toList());
    }

    default boolean isAvailable() {
        return false;
    }
    
    default boolean isEvil() {
        return getAlignment() == Alignment.EVIL;
    }
    
    default boolean isGood() {
        return getAlignment() == Alignment.GOOD;
    }

    default List<CreaturePower> getChampionPowers() {
        return getPowers().stream().filter(CreaturePower::isChampion).collect(Collectors.toList());
    }

    default List<CreaturePower> getGeneralPowers() {
        return getPowers().stream().filter(p -> p.type == PowerType.ABILITY || p.type == PowerType.SPECIAL)
                .collect(Collectors.toList());
    }

    default List<CreaturePower> getAttacks() {
        return getPowers().stream().filter(CreaturePower::isAttack).collect(Collectors.toList());
    }
}
