package net.shoreline.client.impl.event.network;

import net.shoreline.eventbus.event.Event;

/**
 * @author h_ypi
 */
public class SocketReceivedPacketEvent extends Event {
    private final String nick;
    private final String text;

    public SocketReceivedPacketEvent(String nick, String text) {
        this.nick = nick;
        this.text = text;
    }

    public String getNick() {
        return this.nick;
    }

    public String getText() {
        return this.text;
    }
}
