package net.shoreline.client.socket;

import net.shoreline.client.socket.*;
import net.shoreline.client.socket.exception.SocketNickErrorException;
import net.shoreline.client.impl.event.network.SocketReceivedPacketEvent;
import net.shoreline.client.socket.json.Json;
import net.shoreline.client.socket.json.JsonObject;
import net.shoreline.client.socket.websocket.*;
import net.shoreline.eventbus.EventBus;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketChat extends WebSocketAdapter {
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private WebSocket ws;
    private Parser parser;
    private final String URL;
    private final String CHANNEL;
    private final String NICK;
    private final String PASSWORD;

    public SocketChat(String url, String channel, String nick, String password) throws SocketNickErrorException {
        if (!SocketClientUtility.verifyURI(url)) throw new IllegalArgumentException("URL SOCKET ERROR");
        if (!SocketClientUtility.verifyNick(nick)) throw new SocketNickErrorException();
        this.URL = url.endsWith("/") ? url : url + "/";
        this.CHANNEL = channel;
        this.NICK = nick;
        this.PASSWORD = password;
        this.parser = new Parser();
    }

    private void fetchPing() {
        scheduler.scheduleAtFixedRate(() -> sendString(Json.object().add("cmd", "ping").toString()), 0, 50, TimeUnit.SECONDS);
    }

    public void connect() {
        if (ws != null && ws.isOpen()) {
            disconnect();
            ws = null;
        }
        Logger.getLogger(SocketChat.class.getName()).log(Level.FINE, "@" + CHANNEL + ">> Preparing to connect to {0}", URL);
        try {
            ws = new WebSocketFactory()
                    .setVerifyHostname(false)
                    .createSocket(URL)
                    .setAutoFlush(true)
                    .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                    .addListener(this)
                    .connect();
            fetchPing();
            join();
        }
        catch (OpeningHandshakeException | HostnameUnverifiedException ex) {
            Logger.getLogger(SocketChat.class.getName()).log(Level.SEVERE, "@"+CHANNEL+">> Handshake/Hostname error", ex);
            disconnect();
        }
        catch (WebSocketException ex) {
            Logger.getLogger(SocketChat.class.getName()).log(Level.SEVERE, "@"+CHANNEL+">> WebSocket error", ex);
            disconnect();
        }
        catch (IOException ex) {
            Logger.getLogger(SocketChat.class.getName()).log(Level.SEVERE, "@"+CHANNEL+">> IO error", ex);
            disconnect();
        }
    }

    public void disconnect() {
        if (ws != null) {
            try {
                ws.flush();
                ws.disconnect(WebSocketCloseCode.NORMAL);
            } catch (Exception ignored) {
            }
        }
        ws = null;
        scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void join() {
        String join = Json.object()
                .add("cmd", "join")
                .add("channel", CHANNEL)
                .add("nick", (PASSWORD != null && !PASSWORD.trim().isEmpty()) ? NICK + "#" + PASSWORD : NICK)
                .toString();
        sendString(join);
        Logger.getLogger(SocketChat.class.getName()).log(Level.FINER, "Joined channel {0}", CHANNEL);
    }

    public void send(String text) {
        if (text.length() >= 150) text = text.substring(0, text.length() - 5) + "...";
        String chat = Json.object().add("cmd", "chat").add("text", text).toString();
        sendString(chat);
        EventBus.INSTANCE.dispatch(new SocketReceivedPacketEvent(NICK, text));
    }

    public void sendString(String str) {
        if (ws != null) ws.sendText(str);
    }

    public boolean isConnected() {
        return ws != null && ws.isOpen();
    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        super.onTextMessage(websocket, text);
        parser.parse(text, ev -> {
            if (ev.getCallType() == SocketPackets.CHAT) {
                EventBus.INSTANCE.dispatch(new SocketReceivedPacketEvent(ev.getNick(), ev.getText()));
            }
        });
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException ex) {
        disconnect();
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
        disconnect();
    }

    private static class Parser {
        protected void parse(String msg, ParseListener listener) {
            JsonObject json = Json.parse(msg).asObject();
            SocketPackets ev = null;
            long time = System.currentTimeMillis();
            String cmd = json.getString("cmd", "error");
            switch (cmd) {
                case "chat":
                    if (json.getString("text", null) == null) return;
                    ev = new SocketPackets(SocketPackets.CHAT,
                            json.getString("nick", null),
                            json.getString("trip", null),
                            json.getBoolean("admin", false),
                            json.getBoolean("mod", false),
                            json.getString("text", null),
                            null,
                            json.getLong("time", time)
                    );
                    break;
                case "onlineSet":
                    ev = new SocketPackets(SocketPackets.ONLINE_SET,
                            null, null, false, false, null,
                            Json.object().add("nicks", json.get("nicks")).toString(),
                            json.getLong("time", time)
                    );
                    break;
                case "onlineAdd":
                    ev = new SocketPackets(SocketPackets.ONLINE_ADD,
                            json.getString("nick", null), null, false, false, null, null,
                            json.getLong("time", time)
                    );
                    break;
                case "onlineRemove":
                    ev = new SocketPackets(SocketPackets.ONLINE_REMOVE,
                            json.getString("nick", null), null, false, false, null, null,
                            json.getLong("time", time)
                    );
                    break;
                case "info":
                    String itext = json.getString("text", null);
                    String[] parts = itext.split(" ");
                    if (itext.contains("You invited")) {
                        ev = new SocketPackets(SocketPackets.INVITE,
                                parts[2], null, false, false, itext,
                                Json.object().add("channel", parts[4].substring(1)).toString(),
                                json.getLong("time", time));
                    } else if (itext.contains("invited you")) {
                        ev = new SocketPackets(SocketPackets.INVITED,
                                parts[0], null, false, false, itext,
                                Json.object().add("channel", parts[4].substring(1)).toString(),
                                json.getLong("time", time));
                    } else if (itext.contains("IPs")) {
                        ev = new SocketPackets(SocketPackets.STATS,
                                null, null, false, false, itext,
                                Json.object().add("IPs", parts[0]).add("channels", parts[4]).toString(),
                                json.getLong("time", time)
                        );
                    } else if (itext.contains("Banned")) {
                        ev = new SocketPackets(SocketPackets.BANNED,
                                parts[1], null, false, false, itext, null,
                                json.getLong("time", time)
                        );
                    } else if (itext.contains("Unbanned")) {
                        ev = new SocketPackets(SocketPackets.UNBANNED,
                                parts[1], null, false, false, itext, null,
                                json.getLong("time", time)
                        );
                    } else if (itext.contains("Server broadcast:")) {
                        ev = new SocketPackets(SocketPackets.BROADCAST,
                                null, null, false, false, itext, null,
                                json.getLong("time", time)
                        );
                    } else {
                        ev = new SocketPackets(SocketPackets.INFO,
                                null, null, false, false, itext, null,
                                json.getLong("time", time)
                        );
                    }
                    break;
                case "warn":
                    String wtext = json.getString("text", null);
                    if (wtext.contains("rate-limited or blocked")) {
                        ev = new SocketPackets(SocketPackets.WARN_RATE_LTD,
                                null, null, false, false, wtext, null,
                                json.getLong("time", time)
                        );
                    } else {
                        ev = new SocketPackets(SocketPackets.WARN,
                                null, null, false, false,
                                json.getString("text", null), null,
                                json.getLong("time", time)
                        );
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Error: Unidentified message found while parsing! \n" + msg);
            }
            if (ev != null) listener.afterParsing(ev);
        }
    }

    public interface ParseListener {
        void afterParsing(SocketPackets ev);
    }
}
