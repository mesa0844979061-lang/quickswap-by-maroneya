package com.example.quickswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class QuickSwapClient implements ClientModInitializer {
    private static final int CHEST_SLOT = 6; // ช่องเสื้อเกราะใน PlayerScreenHandler
    private static KeyBinding elytraKey;
    private static KeyBinding totemKey;

    @Override
    public void onInitializeClient() {
        elytraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickswap.elytra", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
                KeyBinding.Category.INVENTORY));
        totemKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickswap.totem", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H,
                KeyBinding.Category.INVENTORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (elytraKey.wasPressed()) swapElytra(client);
            while (totemKey.wasPressed()) equipTotem(client);
        });
    }

    private static boolean isChestplate(ItemStack s) {
        EquippableComponent eq = s.get(DataComponentTypes.EQUIPPABLE);
        return eq != null && eq.slot() == EquipmentSlot.CHEST && !s.isOf(Items.ELYTRA);
    }

    /** สลับปีก <-> เสื้อเกราะ */
    private static void swapElytra(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        boolean wearingElytra = p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        int sync = p.playerScreenHandler.syncId;

        for (int i = 9; i <= 44; i++) { // กระเป๋า + hotbar
            ItemStack s = p.playerScreenHandler.getSlot(i).getStack();
            if (s.isEmpty()) continue;
            boolean match = wearingElytra ? isChestplate(s) : s.isOf(Items.ELYTRA);
            if (match) {
                mc.interactionManager.clickSlot(sync, i, 0, SlotActionType.PICKUP, p);
                mc.interactionManager.clickSlot(sync, CHEST_SLOT, 0, SlotActionType.PICKUP, p);
                mc.interactionManager.clickSlot(sync, i, 0, SlotActionType.PICKUP, p);
                return;
            }
        }
        p.sendMessage(Text.translatable("message.quickswap.elytra_none"), true);
    }

    /** เปิดกระเป๋าแล้วสลับโทเทมเข้ามือรอง */
    private static void equipTotem(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            p.sendMessage(Text.translatable("message.quickswap.totem_already"), true);
            return;
        }
        for (int i = 9; i <= 44; i++) {
            if (p.playerScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                if (!(mc.currentScreen instanceof InventoryScreen)) {
                    mc.setScreen(new InventoryScreen(p));
                }
                // SWAP + button 40 = สลับกับมือรอง
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, i, 40, SlotActionType.SWAP, p);
                return;
            }
        }
        p.sendMessage(Text.translatable("message.quickswap.totem_none"), true);
    }
}
