package com.shake1227.disphone.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransceiverChannel {
    private final String channelId;
    private final ConcurrentHashMap<UUID, String> participants = new ConcurrentHashMap<>();

    public TransceiverChannel(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelId() { return channelId; }
    public String getDiscordId(UUID uuid) { return participants.get(uuid); }
    public void addParticipant(UUID uuid, String discordId) { participants.put(uuid, discordId); }
    public void removeParticipant(UUID uuid) { participants.remove(uuid); }

    public List<OfflinePlayer> getParticipantPlayers() {
        List<OfflinePlayer> players = new ArrayList<>();
        for (UUID uuid : participants.keySet()) {
            players.add(Bukkit.getOfflinePlayer(uuid));
        }
        return players;
    }
}