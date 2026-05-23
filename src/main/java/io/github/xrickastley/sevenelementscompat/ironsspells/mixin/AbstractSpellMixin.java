package io.github.xrickastley.sevenelementscompat.ironsspells.mixin;

import io.github.xrickastley.sevenelementscompat.ironsspells.ActiveElementHolder;
import io.github.xrickastley.sevenelementscompat.ironsspells.SevenElementsCompat;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSpell.class)
public abstract class AbstractSpellMixin {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    @Inject(method = "castSpell", at = @At("HEAD"))
    private void beforeCastSpell(Level world, int spellLevel, ServerPlayer serverPlayer, CastSource castSource, boolean triggerCooldown, CallbackInfo ci) {
        if (serverPlayer != null) {
            LOGGER.info("beforeCastSpell called for player: {}, spellLevel: {}, castSource: {}", serverPlayer.getName().getString(), spellLevel, castSource);
            ItemStack castingItem = ItemStack.EMPTY;
            MagicData magicData = MagicData.getPlayerMagicData(serverPlayer);
            AbstractSpell spell = (AbstractSpell)(Object)this;

            // 1. If cast source is SPELLBOOK, force castingItem to the Curios spellbook slot
            if (castSource == CastSource.SPELLBOOK) {
                try {
                    castingItem = io.redspace.ironsspellbooks.api.util.Utils.getPlayerSpellbookStack(serverPlayer);
                    LOGGER.info("CastSource is SPELLBOOK. Forcing castingItem to spellbook: {}", castingItem);
                } catch (Throwable t) {
                    LOGGER.error("Failed to get spellbook stack for SPELLBOOK cast source", t);
                }
            }

            // 2. Otherwise resolve based on selection slot
            if (castingItem == null || castingItem.isEmpty()) {
                if (magicData != null && magicData.getSyncedData() != null && magicData.getSyncedData().getSpellSelection() != null) {
                    var selection = magicData.getSyncedData().getSpellSelection();
                    String slot = selection.equipmentSlot;
                    LOGGER.info("Resolving casting item from selected slot: {}", slot);
                    if ("spellbook".equals(slot)) {
                        try {
                            castingItem = io.redspace.ironsspellbooks.api.util.Utils.getPlayerSpellbookStack(serverPlayer);
                        } catch (Throwable t) {
                            LOGGER.error("Failed to get spellbook stack from Curios", t);
                        }
                    } else if ("mainhand".equals(slot)) {
                        castingItem = serverPlayer.getMainHandItem();
                    } else if ("offhand".equals(slot)) {
                        castingItem = serverPlayer.getOffhandItem();
                    } else if ("head".equals(slot)) {
                        castingItem = serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
                    } else if ("chest".equals(slot)) {
                        castingItem = serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                    } else if ("legs".equals(slot)) {
                        castingItem = serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
                    } else if ("feet".equals(slot)) {
                        castingItem = serverPlayer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
                    }
                    LOGGER.info("Resolved casting item from slot selection: {}", castingItem);
                }
            }

            // 3. Fallback to getPlayerCastingItem() from MagicData
            if (castingItem == null || castingItem.isEmpty()) {
                if (magicData != null) {
                    castingItem = magicData.getPlayerCastingItem();
                    LOGGER.info("Player casting item from MagicData (fallback): {}", castingItem);
                }
            }

            // 4. Fallback to hand items
            if (castingItem == null || castingItem.isEmpty()) {
                castingItem = serverPlayer.getMainHandItem();
                LOGGER.info("Casting item from Main Hand (fallback): {}", castingItem);
                if (castingItem == null || castingItem.isEmpty()) {
                    castingItem = serverPlayer.getOffhandItem();
                    LOGGER.info("Casting item from Off Hand (fallback): {}", castingItem);
                }
            }
            
            // 5. Fallback to Curios spellbook slot
            if (castingItem == null || castingItem.isEmpty()) {
                try {
                    castingItem = io.redspace.ironsspellbooks.api.util.Utils.getPlayerSpellbookStack(serverPlayer);
                    LOGGER.info("Casting item from Curios Spellbook slot (fallback): {}", castingItem);
                } catch (Throwable t) {
                    LOGGER.error("Failed to get spellbook stack from Curios fallback", t);
                }
            }

            if (castingItem != null && !castingItem.isEmpty()) {
                Object element = null;
                try {
                    boolean isContainer = io.redspace.ironsspellbooks.api.spells.ISpellContainer.isSpellContainer(castingItem);
                    LOGGER.info("Casting item is SpellContainer: {}", isContainer);
                    if (isContainer) {
                        int selectedIndex = -1;
                        var container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(castingItem);

                        // Match spell selection index first
                        if (magicData != null && magicData.getSyncedData() != null && magicData.getSyncedData().getSpellSelection() != null) {
                            var selection = magicData.getSyncedData().getSpellSelection();
                            if (castSource == CastSource.SPELLBOOK || "spellbook".equals(selection.equipmentSlot)) {
                                int tempIndex = selection.index;
                                if (tempIndex >= 0 && tempIndex < container.getMaxSpellCount()) {
                                    var spellData = container.getSpellAtIndex(tempIndex);
                                    if (spellData != null && spellData.getSpell().equals(spell)) {
                                        selectedIndex = tempIndex;
                                        LOGGER.info("Matched spell from player selection index: {}", selectedIndex);
                                    }
                                }
                            }
                        }

                        // Search in container slots if not found
                        if (selectedIndex < 0) {
                            var activeSpells = container.getActiveSpells();
                            for (int i = 0; i < activeSpells.size(); i++) {
                                var spellSlot = activeSpells.get(i);
                                if (spellSlot != null && spellSlot.spellData() != null && spellSlot.spellData().getSpell().equals(spell)) {
                                    selectedIndex = spellSlot.index();
                                    LOGGER.info("Matched spell by searching container slots. Index: {}", selectedIndex);
                                    break;
                                }
                            }
                        }

                        if (selectedIndex >= 0) {
                            var slotInfusion = SevenElementsCompat.getSlotInfusedElement(castingItem, selectedIndex);
                            LOGGER.info("Slot infusion for index {}: {}", selectedIndex, slotInfusion);
                            if (slotInfusion != null) {
                                element = slotInfusion.getElement();
                            }
                        }
                    }
                } catch (Throwable e) {
                    LOGGER.error("Error checking spell container / slot infusion", e);
                }

                if (element == null) {
                    element = SevenElementsCompat.getInfusedElementOfItem(castingItem);
                    LOGGER.info("Fallback infused element of item: {}", element);
                }

                if (element != null) {
                    LOGGER.info("Setting ACTIVE_CAST_ELEMENT to: {}", element);
                    ActiveElementHolder.ACTIVE_CAST_ELEMENT.set(element);
                } else {
                    LOGGER.info("No infused element resolved for this cast.");
                }
            } else {
                LOGGER.info("No casting item found.");
            }
        }
    }

    @Inject(method = "castSpell", at = @At("RETURN"))
    private void afterCastSpell(Level world, int spellLevel, ServerPlayer serverPlayer, CastSource castSource, boolean triggerCooldown, CallbackInfo ci) {
        LOGGER.info("afterCastSpell called. Removing ACTIVE_CAST_ELEMENT.");
        ActiveElementHolder.ACTIVE_CAST_ELEMENT.remove();
    }
}
