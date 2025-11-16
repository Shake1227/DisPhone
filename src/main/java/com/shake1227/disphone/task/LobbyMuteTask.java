package com.shake1227.disphone.task;

import com.shake1227.disphone.DisPhone;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import org.bukkit.scheduler.BukkitRunnable;

public class LobbyMuteTask extends BukkitRunnable {

    private final DisPhone plugin;

    public LobbyMuteTask(DisPhone plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        String lobbyChannelId = plugin.getConfigManager().getLobbyChannelId();
        if (lobbyChannelId == null || lobbyChannelId.equals("YOUR_LOBBY_VOICE_CHANNEL_ID_HERE")) {
            return;
        }

        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) return;

        VoiceChannel lobbyChannel = guild.getVoiceChannelById(lobbyChannelId);
        if (lobbyChannel != null) {
            for (Member member : lobbyChannel.getMembers()) {
                if (member.getVoiceState() != null && !member.getVoiceState().isMuted()) {
                    member.mute(true).queue();
                }
            }
        }
    }
}