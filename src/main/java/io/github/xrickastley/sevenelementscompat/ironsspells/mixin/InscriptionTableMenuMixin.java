package io.github.xrickastley.sevenelementscompat.ironsspells.mixin;

import io.github.xrickastley.sevenelementscompat.ironsspells.SevenElementsCompat;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InscriptionTableMenu.class, remap = false)
public abstract class InscriptionTableMenuMixin {
    @Shadow @Final private Slot spellBookSlot;
    @Shadow @Final private Slot scrollSlot;
    @Shadow @Final private Slot resultSlot;
    @Shadow private int selectedSpellIndex;

    private static final ThreadLocal<io.github.xrickastley.sevenelements.component.ElementalInfusionComponent> TEMP_SCROLL_INFUSION = new ThreadLocal<>();

    @Inject(method = "doInscription", at = @At("HEAD"))
    private void beforeDoInscription(int selectedIndex, CallbackInfo ci) {
        ItemStack scrollStack = scrollSlot.getItem();
        if (scrollStack != null && !scrollStack.isEmpty()) {
            var infusion = SevenElementsCompat.getRealInfusion(scrollStack);
            if (infusion != null) {
                TEMP_SCROLL_INFUSION.set(infusion);
            }
        }
    }

    @Inject(method = "doInscription", at = @At("RETURN"))
    private void afterDoInscription(int selectedIndex, CallbackInfo ci) {
        var infusion = TEMP_SCROLL_INFUSION.get();
        TEMP_SCROLL_INFUSION.remove();

        ItemStack spellBookStack = spellBookSlot.getItem();
        if (spellBookStack != null && !spellBookStack.isEmpty()) {
            if (infusion != null) {
                SevenElementsCompat.setSlotInfusedElement(spellBookStack, selectedIndex, infusion);
            }
            SevenElementsCompat.cleanupSlotInfusedElements(spellBookStack);
        }
    }

    @Inject(method = "setupResultSlot", at = @At("RETURN"))
    private void afterSetupResultSlot(CallbackInfo ci) {
        ItemStack spellBookStack = spellBookSlot.getItem();
        ItemStack resultStack = resultSlot.getItem();

        if (spellBookStack != null && !spellBookStack.isEmpty() && resultStack != null && !resultStack.isEmpty()) {
            if (selectedSpellIndex >= 0) {
                var infusion = SevenElementsCompat.getSlotInfusedElement(spellBookStack, selectedSpellIndex);
                if (infusion != null) {
                    var targetComponent = SevenElementsCompat.getElementalInfusionComponent();
                    if (targetComponent != null) {
                        resultStack.set((net.minecraft.core.component.DataComponentType) targetComponent, infusion);
                    }
                }
            }
        }

        if (spellBookStack != null && !spellBookStack.isEmpty()) {
            SevenElementsCompat.cleanupSlotInfusedElements(spellBookStack);
        }
    }
}
