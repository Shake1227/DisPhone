package com.shake1227.disphone.command;

import com.shake1227.disphone.DisPhone;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GivePhoneCommand implements CommandExecutor {

    private final DisPhone plugin;
    public static final NamespacedKey PHONE_KEY = new NamespacedKey(DisPhone.getInstance(), "disphone_item");

    public GivePhoneCommand(DisPhone plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("disphone.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        ItemStack phone = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = phone.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getItemName());
            meta.setLore(plugin.getConfigManager().getItemLore());
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(PHONE_KEY, PersistentDataType.STRING, "true");
            phone.setItemMeta(meta);

            player.getInventory().addItem(phone);
            player.sendMessage(plugin.getConfigManager().getMessage("phone-given"));
        }
        return true;
    }
}