package com.github.mkouba.ddmwi;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "warband_creature")
public class WarbandCreature extends BaseEntity implements CreatureView {

    @ManyToOne
    @JoinColumn(name = "warband_id")
    public Warband warband;

    @ManyToOne
    @JoinColumn(name = "creature_id")
    public Creature creature;

    /**
     * Warband UI position - creatures are sorted by position ASC
     */
    public Integer position;

    public Warband getWarband() {
        return warband;
    }

    public Creature getCreature() {
        return creature;
    }

    public Integer getPosition() {
        return position;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Creature creature() {
        return creature;
    }

    @Override
    public WarbandCreature asWarbandCreature() {
        return this;
    }

    public boolean hasPosition() {
        return position != null;
    }

}
