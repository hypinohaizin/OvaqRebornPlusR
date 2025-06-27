package net.shoreline.client.impl.manager.player;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.ItemDesyncEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.impl.module.client.AnticheatModule;
import net.shoreline.client.impl.module.combat.ReplenishModule;
import net.shoreline.client.init.Managers;
import net.shoreline.client.mixin.accessor.AccessorBundlePacket;
import net.shoreline.client.util.Globals;
import net.shoreline.client.util.math.timer.CacheTimer;
import net.shoreline.client.util.math.timer.Timer;
import net.shoreline.eventbus.EventBus;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InventoryManager implements Globals
{
    private final List<PreSwapData> swapData = new CopyOnWriteArrayList<>();
    private int slot;

    public InventoryManager()
    {
        EventBus.INSTANCE.subscribe(this);
    }

    @EventListener
    public void onPacketOutBound(final PacketEvent.Outbound event)
    {
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet)
        {
            int packetSlot = packet.getSelectedSlot();
            if (!PlayerInventory.isValidHotbarIndex(packetSlot) || slot == packetSlot)
            {
                event.setCanceled(true);
                return;
            }
            slot = packetSlot;
        }
    }

    @EventListener
    public void onPacketInbound(final PacketEvent.Inbound event)
    {
        if (event.getPacket() instanceof UpdateSelectedSlotS2CPacket packet)
        {
            slot = packet.getSlot();
        }

        if (ReplenishModule.getInstance().isInInventoryScreen() || !AnticheatModule.getInstance().isGrim())
        {
            return;
        }

        if (event.getPacket() instanceof BundleS2CPacket bundle)
        {
            List<Packet<?>> allowedBundle = new ArrayList<>();
            for (Packet<?> p : bundle.getPackets())
            {
                if (!(p instanceof ScreenHandlerSlotUpdateS2CPacket))
                {
                    allowedBundle.add(p);
                }
            }
            ((AccessorBundlePacket) bundle).setIterable(allowedBundle);
        }

        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet)
        {
            int s = packet.getSlot() - 36;
            if (s < 0 || s > 8 || packet.getStack().isEmpty()) return;
            for (PreSwapData data : swapData)
            {
                if (data.getSlot() == s || data.getStarting() == s)
                {
                    ItemStack preStack = data.getPreHolding(s);
                    if (!isEqual(preStack, packet.getStack()))
                    {
                        event.cancel();
                        break;
                    }
                }
            }
        }
    }

    @EventListener
    public void onItemDesync(ItemDesyncEvent event)
    {
        if (isDesynced())
        {
            event.cancel();
            event.setStack(getServerItem());
        }
    }

    @EventListener
    public void onDeath(EntityDeathEvent event)
    {
        if (event.getEntity() == mc.player)
        {
            syncToClient();
        }
    }

    @EventListener
    public void onTick(TickEvent event)
    {
        swapData.removeIf(PreSwapData::isPassedClearTime);
    }

    public void setSlot(final int barSlot)
    {
        if (slot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot))
        {
            setSlotForced(barSlot);
            ItemStack[] hotbarCopy = new ItemStack[9];
            for (int i = 0; i < 9; i++)
                hotbarCopy[i] = mc.player.getInventory().getStack(i);
            swapData.add(new PreSwapData(hotbarCopy, slot, barSlot));
        }
    }

    public void setSlotAlt(final int barSlot)
    {
        if (PlayerInventory.isValidHotbarIndex(barSlot))
        {
            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    barSlot + 36, slot, SlotActionType.SWAP, mc.player);
        }
    }

    public void setClientSlot(final int barSlot)
    {
        if (mc.player.getInventory().selectedSlot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot))
        {
            mc.player.getInventory().selectedSlot = barSlot;
            setSlotForced(barSlot);
        }
    }

    public void setSlotForced(final int barSlot)
    {
        Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(barSlot));
    }

    public void syncToClient()
    {
        if (isDesynced())
        {
            setSlotForced(mc.player.getInventory().selectedSlot);
            for (PreSwapData swapData : swapData)
                swapData.beginClear();
        }
    }

    public boolean isDesynced()
    {
        return mc.player.getInventory().selectedSlot != slot;
    }

    public void closeScreen()
    {
        Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    public int pickupSlot(final int slot)
    {
        return click(slot, 0, SlotActionType.PICKUP);
    }

    public void quickMove(final int slot)
    {
        click(slot, 0, SlotActionType.QUICK_MOVE);
    }

    public void throwSlot(final int slot)
    {
        click(slot, 0, SlotActionType.THROW);
    }

    public int findEmptySlot()
    {
        for (int i = 9; i < 36; i++)
        {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) return i;
        }
        return -999;
    }

    public int click(int slot, int button, SlotActionType type)
    {
        if (slot < 0) return -1;
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedList<Slot> slots = screenHandler.slots;
        int i = slots.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot1 : slots) list.add(slot1.getStack().copy());
        screenHandler.onSlotClick(slot, button, type, mc.player);
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        for (int j = 0; j < i; ++j)
        {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = slots.get(j).getStack();
            if (!ItemStack.areEqual(itemStack, itemStack2))
                int2ObjectMap.put(j, itemStack2.copy());
        }
        mc.player.networkHandler.sendPacket(
                new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(),
                        slot, button, type, screenHandler.getCursorStack().copy(), int2ObjectMap));
        return screenHandler.getRevision();
    }

    public int click2(int slot, int button, SlotActionType type)
    {
        if (slot < 0) return -1;
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedList<Slot> slots = screenHandler.slots;
        int i = slots.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot1 : slots) list.add(slot1.getStack().copy());
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        for (int j = 0; j < i; ++j)
        {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = slots.get(j).getStack();
            if (!ItemStack.areEqual(itemStack, itemStack2))
                int2ObjectMap.put(j, itemStack2.copy());
        }
        mc.player.networkHandler.sendPacket(
                new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(),
                        slot, button, type, screenHandler.getCursorStack().copy(), int2ObjectMap));
        return screenHandler.getRevision();
    }

    public int getServerSlot() { return slot; }
    public int getClientSlot() { return mc.player.getInventory().selectedSlot; }

    public ItemStack getServerItem()
    {
        if (mc.player != null && getServerSlot() != -1)
            return mc.player.getInventory().getStack(getServerSlot());
        return null;
    }

    private boolean isEqual(ItemStack stack1, ItemStack stack2)
    {
        return stack1.getItem().equals(stack2.getItem()) && stack1.getName().equals(stack2.getName());
    }

    public static class PreSwapData
    {
        private final ItemStack[] preHotbar;
        private final int starting;
        private final int swapTo;
        private Timer clearTime;

        public PreSwapData(ItemStack[] preHotbar, int start, int swapTo)
        {
            this.preHotbar = preHotbar;
            this.starting = start;
            this.swapTo = swapTo;
        }

        public void beginClear()
        {
            clearTime = new CacheTimer();
            clearTime.reset();
        }

        public boolean isPassedClearTime()
        {
            return clearTime != null && clearTime.passed(300);
        }

        public ItemStack getPreHolding(int i) { return preHotbar[i]; }
        public int getStarting() { return starting; }
        public int getSlot() { return swapTo; }
    }
}
