package com.github.mkouba.ddmwi;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.github.mkouba.ddmwi.Creature.Faction;

@Converter
public class FactionsConverter implements AttributeConverter<Set<Faction>, String> {

    private static final String SEPARATOR = ":";

    @Override
    public String convertToDatabaseColumn(Set<Faction> attribute) {
        return attribute.stream().map(Object::toString).collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public Set<Faction> convertToEntityAttribute(String dbData) {
        if (dbData.trim().isEmpty()) {
            return EnumSet.noneOf(Faction.class);
        }
        return Arrays.stream(dbData.split(SEPARATOR)).map(Faction::valueOf).collect(() -> EnumSet.noneOf(Faction.class),
                EnumSet::add,
                EnumSet::addAll);
    }

}
