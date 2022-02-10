package com.github.mkouba.ddmwi;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.qute.TemplateEnum;

@Entity
@Table(name = "creature", uniqueConstraints = @UniqueConstraint(name = "creature_name", columnNames = "name"))
public class Creature extends PanacheEntity implements CreatureView {

    @NotNull
    @Size(min = 1, max = 100)
    public String name;

    @Enumerated(EnumType.STRING)
    @NotNull
    public Alignment alignment = Alignment.NEUTRAL;

    @Min(0)
    public int cost;

    @Min(0)
    public int level;

    @Min(0)
    public int hp;

    @Min(0)
    public int ac;

    @Min(0)
    public int ref;

    @Min(0)
    public int will;

    @Min(0)
    public int fort;

    @Min(0)
    public int speed;

    @Enumerated(EnumType.STRING)
    @NotNull
    public MovementMode movementMode = MovementMode.NORMAL;

    // IMPL. NOTE we use a special converter to avoid unwieldy join table which makes it difficult to apply UI filters together with pagination
    @NotEmpty
    @Convert(converter = FactionsConverter.class)
    @Column(name = "factions", length = 50)
    public Set<Faction> factions = EnumSet.noneOf(Faction.class);

    // comma-separated list of of keywords
    @Size(max = 300)
    public String keywords;

    @Min(0)
    public int championRating;

    // Initialize 10 lazy proxies at a time  
    // @BatchSize(size = 10)
    // https://github.com/hibernate/hibernate-reactive/issues/1212
    @OneToMany(mappedBy = "creature", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CreaturePower> powers = new ArrayList<>();
    
    @Size(max = 100)
    public String setInfo;

    @Override
    public Creature creature() {
        return this;
    }

    public void addPower(CreaturePower power) {
        power.creature = this;
        powers.add(power);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Creature other = (Creature) obj;
        return Objects.equals(name, other.name);
    }
    
    public static Creature createDummy(long id) {
        Creature c = new Creature();
        c.id = id;
        return c;
    }

    @TemplateEnum
    public enum Alignment {
        GOOD,
        EVIL,
        NEUTRAL
    }

    @TemplateEnum
    public enum Faction {
        BORDERLANDS,
        CIVILIZATION,
        UNDERDARK,
        WILD
    }

    @TemplateEnum
    public enum MovementMode {
        NORMAL,
        FLIGHT,
        BURROW
    }

}
