package com.github.mkouba.ddmwi;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.mkouba.ddmwi.User.Role;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RolesConverter implements AttributeConverter<Set<Role>, String> {

    private static final String SEPARATOR = ":";

    @Override
    public String convertToDatabaseColumn(Set<Role> attribute) {
        return attribute.stream().map(Object::toString).collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public Set<Role> convertToEntityAttribute(String dbData) {
        if (dbData.trim().isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }
        return Arrays.stream(dbData.split(SEPARATOR)).map(Role::valueOf).collect(() -> EnumSet.noneOf(Role.class),
                EnumSet::add,
                EnumSet::addAll);
    }

}
