package com.github.mkouba.ddmwi;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
@Table(name = "warband_creature")
public class WarbandCreature extends PanacheEntity implements CreatureView {

    @ManyToOne
    @JoinColumn(name = "warband_id")
    public Warband warband;

    @ManyToOne
    @JoinColumn(name = "creature_id")
    public Creature creature;

    public Warband getWarband() {
        return warband;
    }

    public Creature getCreature() {
        return creature;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Creature creature() {
        return creature;
    }

}
