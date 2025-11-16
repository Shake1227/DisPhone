package com.shake1227.disphone.ui;

import com.shake1227.disphone.DisPhone;
import com.shake1227.disphone.command.GivePhoneCommand;
import com.shake1227.disphone.manager.ConfigManager;
import com.shake1227.disphone.manager.DataManager;
import com.shake1227.disphone.utils.TransceiverChannel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUI {

    private static final DisPhone plugin = DisPhone.getInstance();
    private static final ConfigManager configManager = plugin.getConfigManager();
    private static final DataManager dataManager = plugin.getDataManager();

    public static final String MAIN_MENU_TITLE = configManager.getMessage("gui.main.title");
    public static final String CONTACTS_MENU_TITLE = configManager.getMessage("gui.contacts.title");
    public static final String SHARE_CONTACT_TITLE = configManager.getMessage("gui.share.title");
    public static final String IN_CALL_MENU_TITLE = configManager.getMessage("gui.in-call.title");
    public static final String TRANSCEIVER_MENU_TITLE = configManager.getMessage("transceiver.gui.menu.title");
    public static final String WHO_IS_HERE_TITLE = configManager.getMessage("transceiver.gui.who-is-here.title");

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, MAIN_MENU_TITLE);
        ItemStack callItem = createItem(Material.GREEN_WOOL, configManager.getMessage("gui.main.call-item-name"), configManager.getMessageList("gui.main.call-item-lore"));
        ItemStack contactsItem = createItem(Material.BOOK, configManager.getMessage("gui.main.contacts-item-name"), configManager.getMessageList("gui.main.contacts-item-lore"));
        inv.setItem(11, callItem);
        inv.setItem(15, contactsItem);
        player.openInventory(inv);
    }

    public static void openInCallMenu(Player player) {
        plugin.getCallHandler().getDiscordMemberAsync(player, member -> {
            boolean isMuted = false;
            if (member != null && member.getVoiceState() != null) {
                isMuted = member.getVoiceState().isMuted();
            }
            boolean finalIsMuted = isMuted;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Inventory inv = Bukkit.createInventory(player, 27, IN_CALL_MENU_TITLE);
                    ItemStack hangupItem = createItem(Material.BARRIER, configManager.getMessage("gui.in-call.hangup-item-name"), configManager.getMessageList("gui.in-call.hangup-item-lore"));
                    ItemStack inviteItem = createPlayerHead(player, configManager.getMessage("gui.in-call.invite-item-name"), configManager.getMessageList("gui.in-call.invite-item-lore"), null);

                    ItemStack muteToggleItem;
                    if (finalIsMuted) {
                        muteToggleItem = createItem(Material.LIME_WOOL, configManager.getMessage("gui.in-call.unmute-item-name"), configManager.getMessageList("gui.in-call.unmute-item-lore"));
                    } else {
                        muteToggleItem = createItem(Material.RED_WOOL, configManager.getMessage("gui.in-call.mute-item-name"), configManager.getMessageList("gui.in-call.mute-item-lore"));
                    }

                    inv.setItem(11, muteToggleItem);
                    inv.setItem(13, inviteItem);
                    inv.setItem(15, hangupItem);

                    player.openInventory(inv);
                }
            }.runTask(plugin);
        });
    }

    public static void openContactsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, CONTACTS_MENU_TITLE);
        String playerNumber = dataManager.getPhoneNumber(player.getUniqueId());

        if (playerNumber != null) {
            ItemStack ownHead = createPlayerHead(player,
                    configManager.getMessage("gui.contacts.own-contact-name").replace("%player%", player.getName()),
                    configManager.getMessageList("gui.contacts.own-contact-lore"),
                    playerNumber);
            inv.setItem(0, ownHead);
        }

        List<UUID> contacts = dataManager.getContacts(player.getUniqueId());
        int slot = 1;
        for (UUID contactUUID : contacts) {
            if (slot >= 54) break;
            OfflinePlayer contactPlayer = Bukkit.getOfflinePlayer(contactUUID);
            String contactNumber = dataManager.getPhoneNumber(contactUUID);
            if (contactNumber != null) {
                ItemStack contactHead = createPlayerHead(contactPlayer,
                        "&e" + contactPlayer.getName(),
                        new ArrayList<>(),
                        contactNumber);
                inv.setItem(slot, contactHead);
            }
            slot++;
        }
        player.openInventory(inv);
    }

    public static void openShareContactMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, SHARE_CONTACT_TITLE);
        int range = configManager.getContactShareRange();
        List<Player> nearbyPlayers = player.getNearbyEntities(range, range, range).stream()
                .filter(entity -> entity instanceof Player && entity != player)
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());

        List<Player> filteredNearbyPlayers = nearbyPlayers.stream()
                .filter(nearby -> !dataManager.getContacts(player.getUniqueId()).contains(nearby.getUniqueId()))
                .filter(nearby -> Arrays.stream(nearby.getInventory().getContents())
                        .filter(item -> item != null && item.hasItemMeta())
                        .anyMatch(item -> item.getItemMeta().getPersistentDataContainer()
                                .has(GivePhoneCommand.PHONE_KEY, PersistentDataType.STRING)))
                .collect(Collectors.toList());

        if (filteredNearbyPlayers.isEmpty()) {
            ItemStack noPlayersItem = createItem(Material.BARRIER, configManager.getMessage("gui.share.no-players-name"), configManager.getMessageList("gui.share.no-players-lore"));
            inv.setItem(22, noPlayersItem);
        } else {
            int slot = 0;
            for (Player nearby : filteredNearbyPlayers) {
                if (slot >= 54) break;
                ItemStack head = createPlayerHead(nearby,
                        "&a" + nearby.getName(),
                        configManager.getMessageList("gui.share.player-lore"),
                        null);
                inv.setItem(slot, head);
                slot++;
            }
        }
        player.openInventory(inv);
    }

    public static void openTransceiverMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, TRANSCEIVER_MENU_TITLE);
        ItemStack changeFreq = createItem(Material.COMPASS, configManager.getMessage("transceiver.gui.menu.change-freq-name"), configManager.getMessageList("transceiver.gui.menu.change-freq-lore"));
        ItemStack whoIsHere = createPlayerHead(player, configManager.getMessage("transceiver.gui.menu.who-is-here-name"), configManager.getMessageList("transceiver.gui.menu.who-is-here-lore"), null);
        ItemStack disconnect = createItem(Material.BARRIER, configManager.getMessage("transceiver.gui.menu.disconnect-name"), configManager.getMessageList("transceiver.gui.menu.disconnect-lore"));
        inv.setItem(11, changeFreq);
        inv.setItem(13, whoIsHere);
        inv.setItem(15, disconnect);
        player.openInventory(inv);
    }

    public static void openWhoIsHereGUI(Player player, TransceiverChannel channel) {
        Inventory inv = Bukkit.createInventory(player, 54, WHO_IS_HERE_TITLE);
        List<OfflinePlayer> participants = channel.getParticipantPlayers();
        if(participants.isEmpty()){
            ItemStack noOne = createItem(Material.GLASS_PANE, configManager.getMessage("transceiver.gui.who-is-here.no-one"), new ArrayList<>());
            inv.setItem(22, noOne);
        } else {
            for (int i = 0; i < participants.size(); i++) {
                if (i >= 54) break;
                OfflinePlayer p = participants.get(i);
                ItemStack head = createPlayerHead(p, "&a" + p.getName(), new ArrayList<>(), null);
                inv.setItem(i, head);
            }
        }
        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createPlayerHead(OfflinePlayer owner, String name, List<String> baseLore, String phoneNumber) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(owner);
            meta.setDisplayName(configManager.format(name));
            List<String> finalLore = new ArrayList<>(baseLore);
            if (phoneNumber != null) {
                for (String line : configManager.getMessageList("gui.contacts.phone-number-lore")) {
                    finalLore.add(line.replace("%number%", phoneNumber));
                }
            }
            meta.setLore(finalLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}