package com.github.mkouba.ddmwi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Predicate;

import org.hibernate.validator.constraints.Length;

import com.github.mkouba.ddmwi.Creature.Alignment;
import com.github.mkouba.ddmwi.Creature.Faction;

import io.quarkus.qute.Qute;
import io.quarkus.qute.TemplateEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "warband", uniqueConstraints = @UniqueConstraint(name = "user_warband_name", columnNames = { "name", "user_id" }))
public class Warband extends BaseEntity {

    public static final int NOTE_LIMIT = 500;

    @NotEmpty
    @Size(max = 100)
    public String name;

    @OneToMany(mappedBy = "warband", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    public List<WarbandCreature> creatures;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_limit")
    @NotNull
    public PointLimit pointLimit = PointLimit.STANDARD;

    public boolean arena;

    @Length(max = NOTE_LIMIT)
    public String note;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    @Column(name = "public_link")
    public boolean publicLink;

    /**
     * A freestyle warband does not impose any rules and restrictions on how to be built (incl. factions and point limit)
     */
    public boolean freestyle;

    @TemplateEnum
    public enum PointLimit {
        QUICK(100, 8),
        STANDARD(200, 10),
        EPIC(500, 10);

        PointLimit(int value, int numberOfCreatures) {
            this.value = value;
            this.numberOfCreatures = numberOfCreatures;
        }

        public final int value;
        public final int numberOfCreatures;
    }

    public boolean isGood() {
        return getAlignment() == Alignment.GOOD;
    }

    public boolean isEvil() {
        return getAlignment() == Alignment.EVIL;
    }

    public List<WarbandCreature> creatures() {
        return creatures == null ? Collections.emptyList() : creatures;
    }

    public boolean containsCreature(Creature creature) {
        return creatures().stream().map(WarbandCreature::getCreature).filter(c -> c.equals(creature)).findAny().isPresent();
    }

    public int getTotalCost() {
        return creatures().stream().mapToInt(CreatureView::getCost).sum();
    }

    public int getRemainingPoints() {
        return pointLimit.value - getTotalCost();
    }

    public Alignment getAlignment() {
        return getAlignment(null);
    }

    public Alignment getAlignment(Predicate<Creature> ignore) {
        for (WarbandCreature wc : creatures()) {
            Creature creature = wc.creature;
            if (ignore != null && ignore.test(creature)) {
                continue;
            }
            if (creature.alignment != Alignment.NEUTRAL) {
                return creature.alignment;
            }
        }
        return Alignment.NEUTRAL;
    }

    public Set<Faction> getBaseFactions() {
        EnumSet<Faction> base = EnumSet.allOf(Faction.class);
        if (freestyle) {
            return base;
        }
        for (WarbandCreature wc : creatures()) {
            base.retainAll(wc.creature.factions);
        }
        return base;
    }

    public int getTotalHp() {
        return creatures().stream().mapToInt(CreatureView::getHp).sum();
    }

    public int getHighestLevel() {
        return creatures().stream().mapToInt(CreatureView::getLevel).max().orElse(0);
    }

    public int getChampionRating() {
        return creatures().stream().mapToInt(CreatureView::getChampionRating).max().orElse(0);
    }

    public boolean isAlignmentOk(Creature creature) {
        Alignment warbandAlignment = getAlignment();
        if (warbandAlignment != Alignment.NEUTRAL
                && creature.alignment != Alignment.NEUTRAL
                && creature.alignment != warbandAlignment) {
            return false;
        }
        return true;
    }

    public boolean isPointsLimitOk(Creature creature) {
        return getRemainingPoints() >= creature.cost;
    }

    public boolean factionsMatch(Collection<Faction> factions) {
        Set<Faction> base = getBaseFactions();
        base.retainAll(factions);
        return !base.isEmpty();
    }

    public boolean canAddCreature() {
        if (creatures == null || freestyle) {
            return true;
        }
        return creatures.size() < getCreaturesLimit();
    }

    public int getCreaturesLimit() {
        return arena ? 5 : pointLimit.numberOfCreatures;
    }

    public Warband addCreature(Creature creature, boolean validate) {
        if (validate && !freestyle) {
            if (!isAlignmentOk(creature)) {
                throw new IllegalArgumentException(Qute.fmt("Creature alignment [{}] does not match the warband alignment: {}",
                        creature.alignment, getAlignment()));
            } else if (!isPointsLimitOk(creature)) {
                throw new IllegalArgumentException(Qute.fmt("Point limit {} exceeded - remaining points: {}",
                        pointLimit.value, getRemainingPoints()));
            } else if (!factionsMatch(creature.factions)) {
                throw new IllegalArgumentException(
                        Qute.fmt("Creature [{}] factions {} do not match the warband base factions: {}",
                                creature.name, creature.factions, getBaseFactions()));
            } else if (!canAddCreature()) {
                if (arena) {
                    throw new IllegalArgumentException("Your warband can contain a maximum of 5 creatures for Arena scenario");
                } else {
                    throw new IllegalArgumentException(
                            Qute.fmt("Your warband can contain a maximum of {} creatures for point limit {}",
                                    pointLimit.numberOfCreatures, pointLimit.value));
                }
            }
        }
        int position;
        if (creatures == null) {
            creatures = new ArrayList<>();
            position = 0;
        } else if (creatures.isEmpty()) {
            position = 0;
        } else {
            position = creatures.stream().filter(WarbandCreature::hasPosition).mapToInt(WarbandCreature::getPosition).max()
                    .orElseThrow() + 1;
        }
        WarbandCreature warbandCreature = new WarbandCreature();
        warbandCreature.warband = this;
        warbandCreature.creature = creature;
        warbandCreature.position = position;
        creatures.add(warbandCreature);
        return this;
    }

    public Warband addCreature(Creature creature) {
        return addCreature(creature, true);
    }

    public void removeCreature(long id) {
        creatures.removeIf(c -> c.id == id);
    }

    public boolean canMoveLeft(long warbandCreatureId) {
        WarbandCreature prev = null;
        for (ListIterator<WarbandCreature> it = creatures.listIterator(); it.hasNext();) {
            WarbandCreature c = it.next();
            if (c.id.equals(warbandCreatureId)) {
                return prev != null;
            }
            prev = c;
        }
        return false;
    }

    public boolean canMoveRight(long warbandCreatureId) {
        for (ListIterator<WarbandCreature> it = creatures.listIterator(); it.hasNext();) {
            WarbandCreature c = it.next();
            if (c.id.equals(warbandCreatureId)) {
                return it.hasNext();
            }
        }
        return false;
    }

    public void moveLeft(long warbandCreatureId) {
        WarbandCreature prev = null;
        WarbandCreature moved = null;
        int movedIdx = 0;
        for (ListIterator<WarbandCreature> it = creatures.listIterator(); it.hasNext();) {
            WarbandCreature c = it.next();
            if (c.id.equals(warbandCreatureId)) {
                if (prev != null) {
                    Integer currentPosition = c.position;
                    Integer prevPosition = prev.position;
                    if (prevPosition == null) {
                        if (currentPosition != null) {
                            prevPosition = currentPosition - 1;
                        } else {
                            // Both positions are null
                            prevPosition = 1;
                            currentPosition = 0;
                        }
                    } else if (currentPosition == null && prevPosition != null) {
                        currentPosition = prevPosition + 1;
                    }
                    c.position = prevPosition;
                    prev.position = currentPosition;
                    moved = c;
                    movedIdx = it.previousIndex();
                }
                break;
            }
            prev = c;
        }
        if (moved != null) {
            creatures.set(movedIdx, prev);
            creatures.set(movedIdx - 1, moved);
        }
    }

    public void moveRight(long warbandCreatureId) {
        WarbandCreature next = null;
        WarbandCreature moved = null;
        int nextIdx = 0;
        for (ListIterator<WarbandCreature> it = creatures.listIterator(); it.hasNext();) {
            WarbandCreature c = it.next();
            if (c.id.equals(warbandCreatureId)) {
                if (it.hasNext()) {
                    Integer currentPosition = c.position;
                    next = it.next();
                    Integer nextPosition = next.position;
                    if (nextPosition == null) {
                        if (currentPosition != null) {
                            nextPosition = currentPosition + 1;
                        } else {
                            // Both positions are null
                            nextPosition = 0;
                            currentPosition = 1;
                        }
                    } else if (currentPosition == null && nextPosition != null) {
                        currentPosition = nextPosition - 1;
                    }
                    c.position = nextPosition;
                    next.position = currentPosition;
                    moved = c;
                    nextIdx = it.previousIndex();
                }
                break;
            }
        }
        if (moved != null) {
            creatures.set(nextIdx, moved);
            creatures.set(nextIdx - 1, next);
        }
    }

    public Warband setUser(Long userId) {
        User dummyUser = new User();
        dummyUser.id = userId;
        user = dummyUser;
        return this;
    }

}
