package com.github.mkouba.ddmwi;

import io.quarkus.qute.TemplateEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "creature_power")
public class CreaturePower extends BaseEntity {

    public static final int TEXT_LIMIT = 700;

    @ManyToOne
    @JoinColumn(name = "creature_id")
    public Creature creature;

    @NotNull
    public PowerType type;

    @Size(max = TEXT_LIMIT)
    public String text;

    @PositiveOrZero
    public Integer usageLimit;

    public PowerType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public boolean isAttack() {
        return type == PowerType.ATTACK;
    }

    public boolean isAbility() {
        return type == PowerType.ABILITY;
    }

    public boolean isSpecial() {
        return type == PowerType.SPECIAL;
    }

    public boolean isChampion() {
        return type == PowerType.CHAMPION;
    }

    @TemplateEnum
    public enum PowerType {
        ATTACK,
        ABILITY,
        SPECIAL,
        CHAMPION
    }
}
