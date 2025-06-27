package net.shoreline.client.impl.module.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.BooleanConfig;
import net.shoreline.client.api.config.setting.EnumConfig;
import net.shoreline.client.api.config.setting.NumberConfig;
import net.shoreline.client.api.config.setting.StringConfig;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.event.entity.EntityDeathEvent;
import net.shoreline.client.impl.event.network.PacketEvent;
import net.shoreline.client.init.Managers;
import net.shoreline.client.util.chat.ChatUtil;
import net.shoreline.eventbus.annotation.EventListener;

import java.util.Random;

public class AutoEzModule extends ToggleModule {
    public AutoEzModule() {
        super("AutoEz", "idk", ModuleCategory.MISCELLANEOUS);
    }
    private final Random r = new Random();
    private static String[] kouzi = new String[0];
    private int lastNum;
    //
    Config<Integer> rangeConfig = register(new NumberConfig<>("Range", "The range", 10, 1, 30));
    Config<Boolean> killConfig = register(new BooleanConfig("Kill", "Send message on kill", true));
    Config<Mode> killMsgModeConfig = register(new EnumConfig<>("KillMsgMode", "Kill message mode", Mode.Ovaq, Mode.values(), () -> killConfig.getValue()));
    Config<String> customConfig = register(new StringConfig("CustomKill", "Custom kill message", "killed %player%", () -> killMsgModeConfig.getValue() == Mode.Custom && killConfig.getValue()));
    Config<Boolean> popConfig = register(new BooleanConfig("Pop", "Send message on pop", true));
    Config<Mode> popMsgModeConfig = register(new EnumConfig<>("PopMsgMode", "Pop message mode", Mode.Ovaq, Mode.values(), () -> popConfig.getValue()));
    Config<String> popcustomConfig = register(new StringConfig("CustomPop", "Custom pop message", "%player% pop %totem%", () -> popMsgModeConfig.getValue() == Mode.Custom && popConfig.getValue()));
//

    private final String[] ovaq = new String[]{
            "%player% ᴇᴢᴢᴢ ᴏᴠᴀǫᴘʟᴜꜱ ᴏᴡɴꜱ ʏᴏᴜ ᴀɴᴅ ᴀʟʟ!",
            "%player%、負けてどんな気持ち？自分語りしてくれてもいいよ？",
            "あれ？%player%ってもしかしてモニター逆さまに見てるの？ｗ",
            "%player%、今のプレイで家族泣いてるぞｗ",
            "%player%、また負けてるの？才能なさすぎｗｗｗ",
            "%player%、今どんな気持ち？教えて、ねぇｗ",
            "%player%、このゲーム向いてないから引退しな？",
            "え、%player%？今のマジで？逆に才能だわｗｗｗ",
            "%player%、君のプレイでお茶吹いたわｗ",
            "%player%、その動き、AIより弱いぞｗｗ",
            "え？%player%落ちた？俺の勝ちだし当然かｗ",
            "%player%、勝率0%更新中！",
            "%player%、そろそろ寝たほうがいいよｗ",
            "うわぁ…%player%、俺のペットより弱いｗ",
            "%player%、次元が違いすぎるｗｗｗ",
            "え？%player%ってAI？人間のふり上手すぎｗ",
            "%player%、次回は頑張れよｗ（無理そうだけど）",
            "%player%、YAJU＆Uヤジュセンパーイ",
            "%player%、お前弱すぎな114514"
    };


    @EventListener
    public String getInfo() {
        return killMsgModeConfig.getValue().name();
    }


    @Override
    public void onEnable() {
        lastNum = -1;
    }

    @EventListener
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity player) || player == mc.player) {
            return;
        }

        String name = player.getName().getString();
        if (Managers.SOCIAL.isFriend(name)) return;
        if (player.distanceTo(mc.player) > rangeConfig.getValue()) return;

        int totemCount = Managers.TOTEM.getTotems(player);
        Mode mode = killMsgModeConfig.getValue();

        switch (mode) {
            case Ovaq -> {
                int num = r.nextInt(0, ovaq.length);
                if (num == lastNum) num = (num + 1) % ovaq.length;
                lastNum = num;
                send(ovaq[num].replaceAll("%player%", name));
            }
            case NEW -> {
                send("%player%が死んでしまいました-p-".replaceAll("%player%", name).replaceAll("%totem%", String.valueOf(totemCount)));
            }
            case Custom -> {
                send(customConfig.getValue().replaceAll("%player%", name).replaceAll("%totem%", String.valueOf(totemCount)));
            }
        }
    }

    @EventListener
    public void onPacketInbound(PacketEvent.Inbound event) {
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING || !popConfig.getValue()) return;

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player) || entity == mc.player) return;

        String playerName = player.getName().getString();
        if (Managers.SOCIAL.isFriend(playerName)) return;

        int totemCount = Managers.TOTEM.getTotems(player);

        switch (popMsgModeConfig.getValue()) {
            case Ovaq -> {
                int num = r.nextInt(ovaq.length);
                if (num == lastNum) num = (num < ovaq.length - 1) ? num + 1 : 0;
                lastNum = num;
                send(ovaq[num].replaceAll("%player%", playerName));
            }
            case NEW -> {
                send(playerName + " TotemPokkan! " + totemCount);
            }
            case Custom -> {
                send(popcustomConfig.getValue().replaceAll("%player%", playerName).replaceAll("%totem%", String.valueOf(totemCount)));
            }
        }
    }


    private void send(String s){
        ChatUtil.serverSendMessage(s);
    }

    public enum Mode {
        Ovaq,
        NEW,
        Custom

    }
}