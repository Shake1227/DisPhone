package com.shake1227.disphone.handler;

import com.shake1227.disphone.DisPhone;
import com.shake1227.disphone.utils.TransceiverChannel;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransceiverHandler {

    private final DisPhone plugin;
    private final Map<String, TransceiverChannel> activeFrequencies = new HashMap<>();
    private final Map<UUID, String> playerFrequencies = new HashMap<>();
    private final Map<UUID, Boolean> transmittingPlayers = new HashMap<>();

    public TransceiverHandler(DisPhone plugin) {
        this.plugin = plugin;
    }

    public boolean isTransmitting(Player player) {
        return transmittingPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public void joinFrequency(Player player, String frequency) {
        if (plugin.getCallHandler().isInCall(player)) {
            plugin.getCallHandler().hangupCall(player);
        }

        if (isInFrequency(player)) {
            leaveFrequency(player);
        }

        player.sendMessage(plugin.getConfigManager().getMessage("transceiver.connecting").replace("%freq%", frequency));

        String discordId = getDiscordId(player);
        if (discordId == null) return;

        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) return;

        TransceiverChannel channel = activeFrequencies.computeIfAbsent(frequency, k -> {
            String channelName = plugin.getConfigManager().getMessage("transceiver.channel-name")
                    .replace("%player%", player.getName());
            VoiceChannel vc = guild.createVoiceChannel(channelName).complete();
            return new TransceiverChannel(vc.getId());
        });

        VoiceChannel vc = guild.getVoiceChannelById(channel.getChannelId());
        if (vc == null) {
            activeFrequencies.remove(frequency);
            joinFrequency(player, frequency);
            return;
        }

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member != null) {
                guild.moveVoiceMember(member, vc).queue();
                member.mute(true).queue();

                vc.getManager().putMemberPermissionOverride(member.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue();
                channel.addParticipant(player.getUniqueId(), discordId);
                playerFrequencies.put(player.getUniqueId(), frequency);
                transmittingPlayers.put(player.getUniqueId(), false);

                player.sendMessage(plugin.getConfigManager().getMessage("transceiver.connected").replace("%freq%", frequency));
            }
        });
    }

    public void toggleTransmit(Player player) {
        if (!isInFrequency(player)) return;

        boolean isTransmitting = transmittingPlayers.getOrDefault(player.getUniqueId(), false);
        boolean newTransmitState = !isTransmitting;

        TransceiverChannel channel = getChannel(player);
        if (channel == null) return;

        String discordId = channel.getDiscordId(player.getUniqueId());
        if (discordId == null) return;

        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        guild.retrieveMemberById(discordId).queue(member -> {
            if (member != null && member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
                member.mute(!newTransmitState).queue();
                transmittingPlayers.put(player.getUniqueId(), newTransmitState);

                if (newTransmitState) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, PotionEffect.INFINITE_DURATION, 4, false, false));
                    player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 1.0f);
                    player.sendMessage(plugin.getConfigManager().getMessage("transceiver.transmit-start"));
                } else {
                    player.removePotionEffect(PotionEffectType.SLOW);
                    player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, 0.7f);
                    player.sendMessage(plugin.getConfigManager().getMessage("transceiver.transmit-stop"));
                }
            }
        });
    }

    public boolean isInFrequency(Player player) {
        return playerFrequencies.containsKey(player.getUniqueId());
    }

    public TransceiverChannel getChannel(Player player) {
        String frequency = playerFrequencies.get(player.getUniqueId());
        if (frequency == null) return null;
        return activeFrequencies.get(frequency);
    }

    public void leaveFrequency(Player player) {
        if (!isInFrequency(player)) return;

        String frequency = playerFrequencies.get(player.getUniqueId());
        TransceiverChannel channel = activeFrequencies.get(frequency);

        if (isTransmitting(player)) {
            toggleTransmit(player);
        }

        playerFrequencies.remove(player.getUniqueId());
        transmittingPlayers.remove(player.getUniqueId());

        if (channel != null) {
            String discordId = channel.getDiscordId(player.getUniqueId());
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            VoiceChannel lobby = guild.getVoiceChannelById(plugin.getConfigManager().getLobbyChannelId());

            if (discordId != null && lobby != null) {
                guild.retrieveMemberById(discordId).queue(member -> {
                    if (member != null && member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
                        guild.moveVoiceMember(member, lobby).queue();
                        member.mute(false).queue();
                    }
                });
            }
            channel.removeParticipant(player.getUniqueId());
            checkAndDeleteChannel(channel);
        }

        player.removePotionEffect(PotionEffectType.SLOW);
        player.sendMessage(plugin.getConfigManager().getMessage("transceiver.disconnected").replace("%freq%", frequency));
    }

    private void checkAndDeleteChannel(TransceiverChannel channel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Guild guild = DiscordSRV.getPlugin().getMainGuild();
                if (guild == null) return;
                VoiceChannel vc = guild.getVoiceChannelById(channel.getChannelId());
                if (vc != null && vc.getMembers().isEmpty()) {
                    vc.delete().queue();
                    activeFrequencies.values().remove(channel);
                }
            }
        }.runTaskLater(plugin, 20L * 5);
    }

    public void endAllFrequencies() {
        for (TransceiverChannel channel : activeFrequencies.values()) {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild != null) {
                VoiceChannel vc = guild.getVoiceChannelById(channel.getChannelId());
                if (vc != null) {
                    vc.delete().queue();
                }
            }
        }
        activeFrequencies.clear();
        playerFrequencies.clear();
        transmittingPlayers.clear();
    }

    private String getDiscordId(Player player) {
        String id = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (id == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
        }
        return id;
    }
}