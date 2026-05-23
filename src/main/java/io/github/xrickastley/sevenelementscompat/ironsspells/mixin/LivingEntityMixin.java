package io.github.xrickastley.sevenelementscompat.ironsspells.mixin;

import io.github.xrickastley.sevenelementscompat.ironsspells.SevenElementsCompat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LivingEntity.class, priority = 500)
public abstract class LivingEntityMixin {
    @ModifyVariable(
        method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At("HEAD"),
        argsOnly = true
    )
    private DamageSource wrapSpellDamage(DamageSource source) {
        return SevenElementsCompat.wrapSpellDamage(source, (LivingEntity) (Object) this);
    }
}
