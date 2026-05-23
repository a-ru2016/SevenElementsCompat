package io.github.xrickastley.sevenelementscompat.ironsspells.mixin;

import io.github.xrickastley.sevenelementscompat.ironsspells.ActiveElementHolder;
import io.github.xrickastley.sevenelementscompat.ironsspells.InfusedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements InfusedEntity {
    @Unique
    private String sevenelementscompat$infusedElement = null;

    @Unique
    private int sevenelementscompat$lastReactionTick = -999;

    @Override
    public String sevenelementscompat$getInfusedElement() {
        return this.sevenelementscompat$infusedElement;
    }

    @Override
    public void sevenelementscompat$setInfusedElement(String element) {
        this.sevenelementscompat$infusedElement = element;
    }

    @Override
    public int sevenelementscompat$getLastReactionTick() {
        return this.sevenelementscompat$lastReactionTick;
    }

    @Override
    public void sevenelementscompat$setLastReactionTick(int tick) {
        this.sevenelementscompat$lastReactionTick = tick;
    }

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        Object activeElement = ActiveElementHolder.ACTIVE_CAST_ELEMENT.get();
        if (activeElement != null) {
            this.sevenelementscompat$infusedElement = activeElement.toString();
            LOGGER.info("Entity {} spawned. Attached infused element: {}", this.getClass().getName(), this.sevenelementscompat$infusedElement);
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void onSaveWithoutId(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.sevenelementscompat$infusedElement != null) {
            tag.putString("SevenElementsCompatInfusedElement", this.sevenelementscompat$infusedElement);
        }
        if (this.sevenelementscompat$lastReactionTick != -999) {
            tag.putInt("SevenElementsCompatLastReactionTick", this.sevenelementscompat$lastReactionTick);
        }
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("SevenElementsCompatInfusedElement")) {
            this.sevenelementscompat$infusedElement = tag.getString("SevenElementsCompatInfusedElement");
        }
        if (tag.contains("SevenElementsCompatLastReactionTick")) {
            this.sevenelementscompat$lastReactionTick = tag.getInt("SevenElementsCompatLastReactionTick");
        }
    }
}
