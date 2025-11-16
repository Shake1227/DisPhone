package com.shake1227.disphone.handler;

import com.shake1227.disphone.DisPhone;
import com.shake1227.disphone.ui.GUI;
import com.shake1227.disphone.utils.GroupCall;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.GuildVoiceState;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.VoiceChannel;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CallHandler {

    private final DisPhone plugin;
    private final Map<UUID, GroupCall> callSessions = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    public CallHandler(DisPhone plugin) {
        this.plugin = plugin;
    }

    public boolean isInCall(Player player) {
        if (!callSessions.containsKey(player.getUniqueId())) {
            return false;
        }
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            callSessions.remove(player.getUniqueId());
            return false;
        }
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) return false;
        Member member = guild.getMemberById(discordId);
        if (member == null) return false;
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null || !voiceState.inVoiceChannel()) {
            callSessions.remove(player.getUniqueId());
            return false;
        }
        VoiceChannel currentChannel = voiceState.getChannel();
        String lobbyChannelId = plugin.getConfigManager().getLobbyChannelId();
        if (currentChannel.getId().equals(lobbyChannelId)) {
            callSessions.remove(player.getUniqueId());
            return false;
        }
        GroupCall call = callSessions.get(player.getUniqueId());
        if (call != null && currentChannel.getId().equals(call.getChannelId())) {
            return true;
        } else {
            callSessions.remove(player.getUniqueId());
            return false;
        }
    }

    public GroupCall getCall(Player player) {
        return callSessions.get(player.getUniqueId());
    }

    public void startCall(Player caller, Player target) {
        if (plugin.getTransceiverHandler().isInFrequency(caller)) {
            plugin.getTransceiverHandler().leaveFrequency(caller);
        }
        if (plugin.getTransceiverHandler().isInFrequency(target)) {
            plugin.getTransceiverHandler().leaveFrequency(target);
        }

        if (isInCall(caller) || isInCall(target)) {
            caller.sendMessage(plugin.getConfigManager().getMessage("player-in-call"));
            return;
        }
        pendingInvites.put(target.getUniqueId(), caller.getUniqueId());
        caller.sendMessage(plugin.getConfigManager().getMessage("call-sent").replace("%player%", target.getName()));
        TextComponent message = new TextComponent(plugin.getConfigManager().getMessage("call-received").replace("%player%", caller.getName()));
        TextComponent accept = new TextComponent(plugin.getConfigManager().getMessage("call-accept-button"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dp accept"));
        TextComponent deny = new TextComponent(plugin.getConfigManager().getMessage("call-deny-button"));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dp deny"));
        message.addExtra(" "); message.addExtra(accept); message.addExtra(" "); message.addExtra(deny);
        target.spigot().sendMessage(message);
    }

    public void acceptCall(Player target) {
        if (plugin.getTransceiverHandler().isInFrequency(target)) {
            plugin.getTransceiverHandler().leaveFrequency(target);
        }

        if (!pendingInvites.containsKey(target.getUniqueId())) {
            target.sendMessage(plugin.getConfigManager().getMessage("no-incoming-call"));
            return;
        }
        UUID callerUuid = pendingInvites.remove(target.getUniqueId());
        Player caller = Bukkit.getPlayer(callerUuid);
        if (caller == null || !caller.isOnline()) {
            target.sendMessage(plugin.getConfigManager().getMessage("caller-offline"));
            return;
        }

        if (plugin.getTransceiverHandler().isInFrequency(caller)) {
            plugin.getTransceiverHandler().leaveFrequency(caller);
        }

        if (isInCall(caller)) {
            GroupCall call = getCall(caller);
            addPlayerToCall(target, call);
        } else {
            createNewCall(caller, target);
        }
    }

    public void denyCall(Player target) {
        if (!pendingInvites.containsKey(target.getUniqueId())) {
            target.sendMessage(plugin.getConfigManager().getMessage("no-incoming-call"));
            return;
        }
        UUID callerUuid = pendingInvites.remove(target.getUniqueId());
        Player caller = Bukkit.getPlayer(callerUuid);
        target.sendMessage(plugin.getConfigManager().getMessage("call-denied-target"));
        if(caller != null && caller.isOnline()){
            caller.sendMessage(plugin.getConfigManager().getMessage("call-denied-caller").replace("%player%", target.getName()));
        }
    }

    private void createNewCall(Player caller, Player target) {
        String callerDiscordId = getDiscordId(caller);
        String targetDiscordId = getDiscordId(target);
        if (callerDiscordId == null || targetDiscordId == null) return;
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) return;
        String channelName = plugin.getConfigManager().getMessage("call-channel-name").replace("%player%", caller.getName());
        guild.createVoiceChannel(channelName).queue(vc -> {
            GroupCall call = new GroupCall(vc.getId());
            addPlayerToCall(caller, call);
            addPlayerToCall(target, call);
        });
    }

    public void addPlayerToCall(Player player, GroupCall call) {
        String discordId = getDiscordId(player);
        if (discordId == null) return;
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        VoiceChannel vc = guild.getVoiceChannelById(call.getChannelId());
        if (vc == null) return;
        guild.retrieveMemberById(discordId).queue(member -> {
            if (member != null) {
                guild.moveVoiceMember(member, vc).queue(v -> {
                    member.mute(false).queue();
                });
                vc.getManager().putMemberPermissionOverride(member.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue();
                call.addParticipant(player.getUniqueId(), discordId);
                callSessions.put(player.getUniqueId(), call);
                String joinedMessage = plugin.getConfigManager().getMessage("call-member-joined").replace("%player%", player.getName());
                call.broadcast(joinedMessage, plugin);
            }
        });
    }

    public void hangupCall(Player player) {
        if (!callSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-in-call"));
            return;
        }
        GroupCall call = getCall(player);
        removePlayerFromCall(player, call);
    }

    public void removePlayerFromCall(Player player, GroupCall call) {
        String discordId = call.getDiscordId(player.getUniqueId());
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

        call.removeParticipant(player.getUniqueId());
        callSessions.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.sendMessage(plugin.getConfigManager().getMessage("call-ended"));
        }

        String leftMessage = plugin.getConfigManager().getMessage("call-member-left").replace("%player%", player.getName());
        call.broadcast(leftMessage, plugin);

        if (call.getParticipants().size() == 1) {
            UUID lastPlayerUuid = new ArrayList<>(call.getParticipants().keySet()).get(0);
            Player lastPlayer = Bukkit.getPlayer(lastPlayerUuid);
            if (lastPlayer != null && lastPlayer.isOnline()) {
                lastPlayer.sendMessage(plugin.getConfigManager().getMessage("call-ended-solo"));
                removePlayerFromCall(lastPlayer, call);
            }
        } else if (call.isEmpty()) {
            checkAndDeleteChannel(call);
        }
    }

    private void checkAndDeleteChannel(GroupCall call) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Guild guild = DiscordSRV.getPlugin().getMainGuild();
                if (guild == null) return;
                VoiceChannel vc = guild.getVoiceChannelById(call.getChannelId());
                if (vc != null) {
                    vc.getGuild().loadMembers().onSuccess(members -> {
                        if (vc.getMembers().isEmpty()) {
                            vc.delete().queue();
                        }
                    });
                }
            }
        }.runTaskLater(plugin, 20L * 5);
    }

    public void endAllCalls() {
        for(GroupCall call : callSessions.values().stream().distinct().collect(Collectors.toList())) {
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            if (guild != null) {
                VoiceChannel vc = guild.getVoiceChannelById(call.getChannelId());
                if (vc != null) {
                    vc.delete().queue();
                }
            }
        }
        callSessions.clear();
    }

    public void forceToLobbyOnQuit(Player player) {
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) return;
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) return;
        guild.retrieveMemberById(discordId).queue(member -> {
            if (member != null && member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
                VoiceChannel currentChannel = member.getVoiceState().getChannel();
                String lobbyId = plugin.getConfigManager().getLobbyChannelId();
                if (currentChannel != null && !currentChannel.getId().equals(lobbyId)) {
                    VoiceChannel lobbyChannel = guild.getVoiceChannelById(lobbyId);
                    if (lobbyChannel != null) {
                        guild.moveVoiceMember(member, lobbyChannel).queue();
                    }
                }
            }
        });
    }

    public void inviteToCall(Player inviter, Player target) {
        if (!isInCall(inviter)) {
            inviter.sendMessage(plugin.getConfigManager().getMessage("not-in-call-invite"));
            return;
        }
        if (isInCall(target)) {
            inviter.sendMessage(plugin.getConfigManager().getMessage("player-in-call"));
            return;
        }
        pendingInvites.put(target.getUniqueId(), inviter.getUniqueId());
        inviter.sendMessage(plugin.getConfigManager().getMessage("invite-sent").replace("%player%", target.getName()));
        TextComponent message = new TextComponent(plugin.getConfigManager().getMessage("invite-received").replace("%player%", inviter.getName()));
        TextComponent accept = new TextComponent(plugin.getConfigManager().getMessage("call-accept-button"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dp accept"));
        TextComponent deny = new TextComponent(plugin.getConfigManager().getMessage("call-deny-button"));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dp deny"));
        message.addExtra(" "); message.addExtra(accept); message.addExtra(" "); message.addExtra(deny);
        target.spigot().sendMessage(message);
    }

    public void getDiscordMemberAsync(Player player, Consumer<Member> callback) {
        String discordId = getDiscordId(player);
        if (discordId == null) {
            callback.accept(null);
            return;
        }
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        if (guild == null) {
            callback.accept(null);
            return;
        }
        guild.retrieveMemberById(discordId).queue(callback::accept, throwable -> callback.accept(null));
    }

    public void toggleMute(Player player) {
        if (!isInCall(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-in-call"));
            return;
        }
        getDiscordMemberAsync(player, member -> {
            if (member != null && member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
                boolean currentMuteState = member.getVoiceState().isMuted();
                boolean newMuteState = !currentMuteState;
                member.mute(newMuteState).queue(success -> {
                    player.sendMessage(plugin.getConfigManager().getMessage(newMuteState ? "mute-success" : "unmute-success"));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && isInCall(player)) {
                                GUI.openInCallMenu(player);
                            }
                        }
                    }.runTaskLater(plugin, 10L);
                });
            }
        });
    }

    private String getDiscordId(Player player) {
        String id = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (id == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
        }
        return id;
    }
}