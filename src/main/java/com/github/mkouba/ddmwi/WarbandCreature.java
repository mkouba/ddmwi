package com.github.mkouba.ddmwi;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
