package com.shake1227.disphone.command;

import com.shake1227.disphone.DisPhone;
import com.shake1227.disphone.handler.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DisPhoneCommand implements CommandExecutor, TabCompleter {

    private final DisPhone plugin;
    public static final NamespacedKey TRANSCEIVER_KEY = new NamespacedKey(DisPhone.getInstance(), "disphone_transceiver");


    public DisPhoneCommand(DisPhone plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("command-usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept":
                plugin.getCallHandler().acceptCall(player);
                break;
            case "deny":
                plugin.getCallHandler().denyCall(player);
                break;
            case "hangup":
                plugin.getCallHandler().hangupCall(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("invite-usage"));
                    return true;
                }
                String phoneNumber = args[1];
                String formattedInput = phoneNumber.replace("-", "");
                if (formattedInput.length() == 11 && formattedInput.startsWith("020")) {
                    UUID targetUUID = plugin.getDataManager().getPlayerByPhoneNumber(phoneNumber);
                    if (targetUUID != null) {
                        Player target = Bukkit.getPlayer(targetUUID);
                        if (target != null && target.isOnline()) {
                            plugin.getCallHandler().inviteToCall(player, target);
                        } else {
                            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found-offline"));
                        }
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-call-format"));
                }
                break;
            case "mutetoggle":
                plugin.getCallHandler().toggleMute(player);
                break;
            case "shareaccept":
                handleShareAccept(player);
                break;
            case "givetransceiver":
                if (!player.hasPermission("disphone.admin")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                giveTransceiver(player);
                break;
            default:
                player.sendMessage(plugin.getConfigManager().getMessage("command-usage"));
                break;
        }
        return true;
    }

    private void giveTransceiver(Player player) {
        ItemStack transceiver = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = transceiver.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getMessage("transceiver.item-name"));
            meta.setLore(plugin.getConfigManager().getMessageList("transceiver.item-lore"));
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(TRANSCEIVER_KEY, PersistentDataType.STRING, "true");
            transceiver.setItemMeta(meta);
            player.getInventory().addItem(transceiver);
            player.sendMessage(plugin.getConfigManager().getMessage("transceiver.given"));
        }
    }

    private void handleShareAccept(Player target) {
        PlayerListener listener = plugin.getPlayerListener();
        Map<UUID, UUID> shareRequests = listener.getShareRequests();

        if (!shareRequests.containsKey(target.getUniqueId())) {
            target.sendMessage(plugin.getConfigManager().getMessage("share-no-request"));
            return;
        }

        UUID sharerUuid = shareRequests.remove(target.getUniqueId());
        Player sharer = Bukkit.getPlayer(sharerUuid);

        if (sharer == null || !sharer.isOnline()) {
            target.sendMessage(plugin.getConfigManager().getMessage("share-sharer-offline"));
            return;
        }

        plugin.getDataManager().addContact(target.getUniqueId(), sharer.getUniqueId());
        plugin.getDataManager().addContact(sharer.getUniqueId(), target.getUniqueId());

        target.sendMessage(plugin.getConfigManager().getMessage("share-accepted-target").replace("%player%", sharer.getName()));
        sharer.sendMessage(plugin.getConfigManager().getMessage("share-accepted-sharer").replace("%player%", target.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("accept", "deny", "hangup", "invite", "mutetoggle", "shareaccept", "givetransceiver").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}