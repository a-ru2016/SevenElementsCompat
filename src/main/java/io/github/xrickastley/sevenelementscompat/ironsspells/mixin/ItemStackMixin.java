package io.github.xrickastley.sevenelementscompat.ironsspells.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.xrickastley.sevenelementscompat.ironsspells.SevenElementsCompat;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;
import java.util.Set;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    private static final ThreadLocal<Boolean> IN_COMPAT_LOOKUP = ThreadLocal.withInitial(() -> false);

    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "getComponents", at = @At("RETURN"))
    private DataComponentMap wrapComponents(DataComponentMap original) {
        if (IN_COMPAT_LOOKUP.get()) {
            return original;
        }

        final ItemStack stack = (ItemStack) (Object) this;
        
        IN_COMPAT_LOOKUP.set(true);
        boolean hasInfusion;
        try {
            hasInfusion = SevenElementsCompat.hasDynamicInfusion(stack);
        } finally {
            IN_COMPAT_LOOKUP.set(false);
        }

        if (!hasInfusion) {
            return original;
        }

        final DataComponentType<?> targetComponent = SevenElementsCompat.getElementalInfusionComponent();
        if (targetComponent == null) {
            return original;
        }

        return new DataComponentMap() {
            @Override
            public <T> T get(DataComponentType<? extends T> componentType) {
                if (componentType == targetComponent) {
                    T result = original.get(componentType);
                    if (result == null) {
                        IN_COMPAT_LOOKUP.set(true);
                        try {
                            return (T) SevenElementsCompat.getDynamicInfusion(stack);
                        } finally {
                            IN_COMPAT_LOOKUP.set(false);
                        }
                    }
                    return result;
                }
                return original.get(componentType);
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                Set<DataComponentType<?>> originalKeys = original.keySet();
                if (!originalKeys.contains(targetComponent)) {
                    Set<DataComponentType<?>> newKeys = new HashSet<>(originalKeys);
                    newKeys.add((DataComponentType<?>) targetComponent);
                    return newKeys;
                }
                return originalKeys;
            }
        };
    }
}
