package com.shake1227.disphone;

import com.shake1227.disphone.command.DisPhoneCommand;
import com.shake1227.disphone.command.GivePhoneCommand;
import com.shake1227.disphone.handler.CallHandler;
import com.shake1227.disphone.handler.PlayerListener;
import com.shake1227.disphone.handler.TransceiverHandler;
import com.shake1227.disphone.manager.ConfigManager;
import com.shake1227.disphone.manager.DataManager;
import com.shake1227.disphone.task.LobbyMuteTask; // 追加
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask; // 追加

public final class DisPhone extends JavaPlugin {

    private static DisPhone instance;
    private DataManager dataManager;
    private ConfigManager configManager;
    private CallHandler callHandler;
    private TransceiverHandler transceiverHandler;
    private PlayerListener playerListener;
    private BukkitTask lobbyMuteTask;
    @Override
    public void onEnable() {
        instance = this;

        if (getServer().getPluginManager().getPlugin("DiscordSRV") == null) {
            getLogger().severe("DiscordSRVが見つかりません！ DisPhoneを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.callHandler = new CallHandler(this);
        this.transceiverHandler = new TransceiverHandler(this);
        this.playerListener = new PlayerListener(this);

        this.getCommand("givephone").setExecutor(new GivePhoneCommand(this));
        this.getCommand("disphone").setExecutor(new DisPhoneCommand(this));

        this.getServer().getPluginManager().registerEvents(playerListener, this);

        this.lobbyMuteTask = new LobbyMuteTask(this).runTaskTimerAsynchronously(this, 0L, 100L);

        getLogger().info("DisPhoneが有効になりました！");
    }

    @Override
    public void onDisable() {
        if (callHandler != null) {
            callHandler.endAllCalls();
        }
        if (transceiverHandler != null) {
            transceiverHandler.endAllFrequencies();
        }
        if (lobbyMuteTask != null && !lobbyMuteTask.isCancelled()) {
            lobbyMuteTask.cancel();
        }
        getLogger().info("DisPhoneが無効になりました。");
    }

    public static DisPhone getInstance() {
        return instance;
    }
    public DataManager getDataManager() {
        return dataManager;
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
    public CallHandler getCallHandler() {
        return callHandler;
    }
    public TransceiverHandler getTransceiverHandler() {
        return transceiverHandler;
    }
    public PlayerListener getPlayerListener() {
        return playerListener;
    }
}