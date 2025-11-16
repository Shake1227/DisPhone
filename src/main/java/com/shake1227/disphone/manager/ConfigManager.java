package com.shake1227.disphone.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;
    private String prefix;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        this.prefix = ChatColor.translateAlternateColorCodes('&', messages.getString("prefix", "&b[DisPhone] &r"));
    }

    public String getMessage(String path) {
        String msg = messages.getString(path, "&cMessage not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', msg.replace("%prefix%", this.prefix));
    }

    public List<String> getMessageList(String path) {
        return messages.getStringList(path).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line.replace("%prefix%", this.prefix)))
                .collect(Collectors.toList());
    }

    public String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getItemName() {
        return format(config.getString("item.name"));
    }

    public List<String> getItemLore() {
        return config.getStringList("item.lore").stream()
                .map(this::format)
                .collect(Collectors.toList());
    }

    public String getLobbyChannelId() {
        return config.getString("discord.lobby-voice-channel-id");
    }

    public int getContactShareRange() {
        return config.getInt("phone.contact-share-range", 10);
    }
}