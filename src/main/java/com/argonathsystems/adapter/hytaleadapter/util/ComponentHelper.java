package com.argonathsystems.adapter.hytaleadapter.util;

import com.hytale.api.component.Component;
import com.hytale.api.entity.Entity;

import java.util.Optional;

public class ComponentHelper {
    public static <T> Optional<T> getComponent(Object entity, Class<T> componentClass) {
        if (entity instanceof Entity) {
            return Optional.ofNullable(((Entity) entity).getComponent(componentClass));
        }
        return Optional.empty();
    }
}