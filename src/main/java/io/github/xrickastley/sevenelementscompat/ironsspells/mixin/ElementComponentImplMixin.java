package io.github.xrickastley.sevenelementscompat.ironsspells.mixin;

import io.github.xrickastley.sevenelementscompat.ironsspells.SevenElementsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

@Mixin(targets = "io.github.xrickastley.sevenelements.component.ElementComponentImpl", remap = false)
public abstract class ElementComponentImplMixin {

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "applyFromDamageSource(Lio/github/xrickastley/sevenelements/element/ElementalDamageSource;)Ljava/util/List;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void wrapSpellDamageSource(io.github.xrickastley.sevenelements.element.ElementalDamageSource source, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<java.util.List<?>> cir) {
        if (source == null) return;

        Object finalSource = source;
        // もし元素が PHYSICAL である場合、元の DamageSource を調べて再ラップを試みる
        if (isPhysicalElement(source)) {
            DamageSource original = getOriginalSourceSafely(source);
            if (original != null) {
                LivingEntity owner = getOwnerSafely();
                if (owner != null) {
                    DamageSource wrapped = SevenElementsCompat.wrapSpellDamage(original, owner);
                    if (wrapped != null && wrapped.getClass().getName().equals("io.github.xrickastley.sevenelements.element.ElementalDamageSource")) {
                        finalSource = wrapped;
                    }
                }
            }
        }

        try {
            Object application = finalSource.getClass().getMethod("getElementalApplication").invoke(finalSource);
            Object icdContext = finalSource.getClass().getMethod("getIcdContext").invoke(finalSource);

            java.lang.reflect.Method addAppMethod = this.getClass().getMethod(
                "addElementalApplication", 
                Class.forName("io.github.xrickastley.sevenelements.element.ElementalApplication"),
                Class.forName("io.github.xrickastley.sevenelements.element.InternalCooldownContext")
            );
            Object reactionList = addAppMethod.invoke(this, application, icdContext);
            if (reactionList instanceof java.util.List<?> list) {
                cir.setReturnValue(list);
            }
        } catch (Exception e) {
            // Fallback to default execution if reflection fails
        }
    }

    private boolean isPhysicalElement(Object source) {
        try {
            Object app = source.getClass().getMethod("getElementalApplication").invoke(source);
            if (app != null) {
                Object elementObj = app.getClass().getMethod("getElement").invoke(app);
                if (elementObj != null) {
                    String name = (String) elementObj.getClass().getMethod("name").invoke(elementObj);
                    return "PHYSICAL".equals(name);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private LivingEntity getOwnerSafely() {
        try {
            java.lang.reflect.Method getOwnerMethod = this.getClass().getMethod("getOwner");
            Object res = getOwnerMethod.invoke(this);
            if (res instanceof LivingEntity le) {
                return le;
            }
        } catch (Exception e) {
            try {
                Class<?> clazz = Class.forName("io.github.xrickastley.sevenelements.component.ElementComponentImpl");
                java.lang.reflect.Field ownerField = clazz.getDeclaredField("owner");
                ownerField.setAccessible(true);
                Object res = ownerField.get(this);
                if (res instanceof LivingEntity le) {
                    return le;
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        return null;
    }

    private DamageSource getOriginalSourceSafely(Object eds) {
        try {
            java.lang.reflect.Method m = eds.getClass().getMethod("getOriginalSource");
            Object res = m.invoke(eds);
            if (res instanceof DamageSource ds) {
                return ds;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
