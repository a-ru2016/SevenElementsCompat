package io.github.xrickastley.sevenelementscompat.ironsspells;

import net.minecraft.world.entity.LivingEntity;

public class ActiveElementHolder {
    public static final ThreadLocal<Object> ACTIVE_CAST_ELEMENT = new ThreadLocal<>();
    public static final ThreadLocal<LivingEntity> ACTIVE_CAST_CASTER = new ThreadLocal<>();
    public static final ThreadLocal<Boolean> BYPASS_DYNAMIC_INFUSION = ThreadLocal.withInitial(() -> false);
}
