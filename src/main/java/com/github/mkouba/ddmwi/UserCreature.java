package com.github.mkouba.ddmwi;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
@Table(name = "user_creature")
public class UserCreature extends PanacheEntity implements CreatureView {

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne
    @JoinColumn(name = "creature_id")
    public Creature creature;

    @Override
    public Creature creature() {
        return creature;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

}
