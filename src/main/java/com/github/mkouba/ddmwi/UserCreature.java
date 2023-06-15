package com.github.mkouba.ddmwi;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_creature")
public class UserCreature extends BaseEntity implements CreatureView {

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
