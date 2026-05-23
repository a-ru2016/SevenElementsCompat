package io.github.xrickastley.sevenelementscompat.ironsspells.mixin;

import io.github.xrickastley.sevenelementscompat.ironsspells.ActiveElementHolder;
import io.github.xrickastley.sevenelementscompat.ironsspells.InfusedEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "onHit", at = @At("HEAD"))
    private void beforeOnHit(HitResult result, CallbackInfo ci) {
        if ((Object) this instanceof InfusedEntity ie) {
            String elementName = ie.sevenelementscompat$getInfusedElement();
            if (elementName != null) {
                try {
                    Class<?> elementClass = Class.forName("io.github.xrickastley.sevenelements.element.Element");
                    Object element = Enum.valueOf((Class<Enum>) elementClass, elementName);
                    ActiveElementHolder.ACTIVE_CAST_ELEMENT.set(element);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    @Inject(method = "onHit", at = @At("RETURN"))
    private void afterOnHit(HitResult result, CallbackInfo ci) {
        ActiveElementHolder.ACTIVE_CAST_ELEMENT.remove();
    }
}
