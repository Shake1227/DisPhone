package com.shake1227.disphone.utils;

import com.shake1227.disphone.DisPhone;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GroupCall {

    private final String channelId;
    private final Map<UUID, String> participants = new ConcurrentHashMap<>();

    public GroupCall(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void addParticipant(UUID uuid, String discordId) {
        participants.put(uuid, discordId);
    }

    public void removeParticipant(UUID uuid) {
        participants.remove(uuid);
    }

    public Map<UUID, String> getParticipants() {
        return participants;
    }

    public String getDiscordId(UUID uuid) {
        return participants.get(uuid);
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    public void broadcast(String message, DisPhone plugin) {
        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }
}