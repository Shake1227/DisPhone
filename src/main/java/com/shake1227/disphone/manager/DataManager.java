package com.shake1227.disphone.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataManager {

    private final JavaPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml!");
        }
    }

    public String getPhoneNumber(UUID uuid) {
        return dataConfig.getString("players." + uuid.toString() + ".phone-number");
    }

    public void setPhoneNumber(UUID uuid, String number) {
        String formattedNumber = "020-" + number.substring(0, 4) + "-" + number.substring(4);
        dataConfig.set("players." + uuid.toString() + ".phone-number", formattedNumber);
        saveData();
    }

    public boolean phoneNumberExists(String number) {
        String formattedNumber = "020-" + number.substring(0, 4) + "-" + number.substring(4);
        if (dataConfig.getConfigurationSection("players") == null) return false;

        for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
            if (formattedNumber.equals(dataConfig.getString("players." + key + ".phone-number"))) {
                return true;
            }
        }
        return false;
    }

    public List<UUID> getContacts(UUID uuid) {
        return dataConfig.getStringList("players." + uuid.toString() + ".contacts").stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    public void addContact(UUID playerUuid, UUID targetUuid) {
        List<String> contacts = dataConfig.getStringList("players." + playerUuid.toString() + ".contacts");
        if (!contacts.contains(targetUuid.toString())) {
            contacts.add(targetUuid.toString());
            dataConfig.set("players." + playerUuid.toString() + ".contacts", contacts);
            saveData();
        }
    }

    public UUID getPlayerByPhoneNumber(String phoneNumber) {
        if (dataConfig.getConfigurationSection("players") == null) return null;

        for (String uuidString : dataConfig.getConfigurationSection("players").getKeys(false)) {
            if (phoneNumber.equals(dataConfig.getString("players." + uuidString + ".phone-number"))) {
                return UUID.fromString(uuidString);
            }
        }
        return null;
    }
}