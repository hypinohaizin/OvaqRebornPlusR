package net.shoreline.client.impl.module.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

public class ToolSaverModule extends ToggleModule {
    Config<Integer> savePercent = register(new NumberConfig<>("SavePercent", "Percent at which to auto-save tool", 10, 1, 50));
    private boolean warned = false;

    public ToolSaverModule() {
        super("ToolSaver", "Auto-swap tool when durability is low", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        ItemStack main = mc.player.getMainHandStack();
        if (!(main.getItem() instanceof MiningToolItem) || main.isEmpty()) {
            warned = false;
            return;
        }

        int maxDamage = main.getMaxDamage();
        int durability = maxDamage - main.getDamage();
        int percent = (int) ((durability / (float) maxDamage) * 100F);

        if (percent <= savePercent.getValue()) {
            if (!warned) {
                int swap = findSpareToolSlot(main);
                if (swap != -1 && swap != Managers.INVENTORY.getClientSlot()) {
                    mc.player.getInventory().selectedSlot = swap;
                    Managers.INVENTORY.setSlot(swap);
                    ChatUtil.clientSendMessage("§c[ToolSaver] Durability is dangerous, so it automatically switched to another tool.");
                    warned = true;
                } else {
                    ChatUtil.clientSendMessage("§c[ToolSaver] Durability is dangerous, but no spare tools can be found.");
                    warned = true;
                }
            }
        } else {
            warned = false;
        }
    }

    private int findSpareToolSlot(ItemStack main) {
        int selected = Managers.INVENTORY.getClientSlot();
        int maxPercent = savePercent.getValue();
        for (int i = 0; i < 9; i++) {
            if (i == selected) continue;
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof MiningToolItem)) continue;
            // 種類が同じツール限定にしたいなら下を有効化
            // if (!stack.getItem().equals(main.getItem())) continue;
            int maxDamage = stack.getMaxDamage();
            int durability = maxDamage - stack.getDamage();
            int percent = (int) ((durability / (float) maxDamage) * 100F);
            if (percent > maxPercent) {
                return i;
            }
        }
        return -1;
    }
}
