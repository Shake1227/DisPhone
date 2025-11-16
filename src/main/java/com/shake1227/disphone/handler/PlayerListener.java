package com.shake1227.disphone.handler;

import com.shake1227.disphone.DisPhone;
import com.shake1227.disphone.command.DisPhoneCommand;
import com.shake1227.disphone.command.GivePhoneCommand;
import com.shake1227.disphone.ui.AnvilInput;
import com.shake1227.disphone.ui.GUI;
import com.shake1227.disphone.utils.TransceiverChannel;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final DisPhone plugin;
    private final Map<UUID, UUID> shareRequests = new HashMap<>();

    public PlayerListener(DisPhone plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        // DisPhoneの処理
        if (item.getItemMeta().getPersistentDataContainer().has(GivePhoneCommand.PHONE_KEY, PersistentDataType.STRING)) {
            if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

            event.setCancelled(true);
            if (plugin.getDataManager().getPhoneNumber(player.getUniqueId()) == null) {
                AnvilInput.openRegistrationGUI(player);
                return;
            }
            if (plugin.getCallHandler().isInCall(player)) {
                GUI.openInCallMenu(player);
            } else {
                GUI.openMainMenu(player);
            }
        }

        // Transceiverの処理
        if (item.getItemMeta().getPersistentDataContainer().has(DisPhoneCommand.TRANSCEIVER_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            TransceiverHandler transceiverHandler = plugin.getTransceiverHandler();

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (transceiverHandler.isInFrequency(player)) {
                    transceiverHandler.toggleTransmit(player);
                } else {
                    AnvilInput.openFrequencyGUI(player);
                }
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (transceiverHandler.isInFrequency(player)) {
                    GUI.openTransceiverMenu(player);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("transceiver.not-connected"));
                }
            }
        }
    }

    // ### ここからが追加/修正箇所 ###

    /**
     * トランシーバーをホットバーから外した際に自動でミュートする処理
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        TransceiverHandler transceiverHandler = plugin.getTransceiverHandler();

        // 交信状態かチェック
        if (transceiverHandler.isTransmitting(player)) {
            ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
            if (previousItem != null && previousItem.hasItemMeta() &&
                    previousItem.getItemMeta().getPersistentDataContainer().has(DisPhoneCommand.TRANSCEIVER_KEY, PersistentDataType.STRING)) {
                // 手に持っていたのがトランシーバーならミュート（交信終了）
                transceiverHandler.toggleTransmit(player);
            }
        }
    }

    /**
     * トランシーバーを捨てた際に自動でミュートする処理
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        TransceiverHandler transceiverHandler = plugin.getTransceiverHandler();

        if (transceiverHandler.isTransmitting(player)) {
            ItemStack droppedItem = event.getItemDrop().getItemStack();
            if (droppedItem.hasItemMeta() &&
                    droppedItem.getItemMeta().getPersistentDataContainer().has(DisPhoneCommand.TRANSCEIVER_KEY, PersistentDataType.STRING)) {
                transceiverHandler.toggleTransmit(player);
            }
        }
    }

    /**
     * プレイヤーがサーバーから退出した際の処理
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();

        // 電話中の場合、ハングアップ処理（ロビーに戻す処理も含まれる）
        if(plugin.getCallHandler().isInCall(player)){
            plugin.getCallHandler().hangupCall(player);
        }

        // 無線接続中の場合、切断処理（ロビーに戻す処理も含まれる）
        if (plugin.getTransceiverHandler().isInFrequency(player)) {
            plugin.getTransceiverHandler().leaveFrequency(player);
        }

        // 状態がズレてVCに置き去りにされている場合のフェイルセーフ
        plugin.getCallHandler().forceToLobbyOnQuit(player);
    }

    // ### ここまでが追加/修正箇所 ###


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (title.equals(GUI.MAIN_MENU_TITLE)) {
            event.setCancelled(true);
            switch (event.getRawSlot()) {
                case 11:
                    player.closeInventory();
                    AnvilInput.openCallGUI(player);
                    break;
                case 15:
                    GUI.openContactsMenu(player);
                    break;
            }
        }
        else if (title.equals(GUI.CONTACTS_MENU_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 0) {
                GUI.openShareContactMenu(player);
            } else {
                if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta()) {
                    SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                    if (meta.getOwningPlayer() != null) {
                        OfflinePlayer target = meta.getOwningPlayer();
                        Player onlineTarget = target.getPlayer();
                        if (onlineTarget != null && onlineTarget.isOnline()) {
                            plugin.getCallHandler().startCall(player, onlineTarget);
                            player.closeInventory();
                        } else {
                            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found-offline"));
                        }
                    }
                }
            }
        }
        else if (title.equals(GUI.SHARE_CONTACT_TITLE)) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta()) {
                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                if (meta.getOwningPlayer() != null) {
                    Player target = meta.getOwningPlayer().getPlayer();
                    if (target != null && target.isOnline()) {
                        player.closeInventory();
                        sendShareRequest(player, target);
                    }
                }
            }
        }
        else if (title.equals(GUI.IN_CALL_MENU_TITLE)) {
            event.setCancelled(true);
            switch(event.getRawSlot()) {
                case 11: // Mute/Unmute Toggle
                    player.performCommand("dp mutetoggle");
                    break;
                case 13: // Invite
                    player.closeInventory();
                    AnvilInput.openInviteGUI(player);
                    break;
                case 15: // Hangup
                    player.closeInventory();
                    player.performCommand("dp hangup");
                    break;
            }
        }
        else if (title.equals(GUI.TRANSCEIVER_MENU_TITLE)) {
            event.setCancelled(true);
            switch(event.getRawSlot()) {
                case 11: // 周波数変更
                    player.closeInventory();
                    AnvilInput.openFrequencyGUI(player);
                    break;
                case 13: // 参加者表示
                    TransceiverChannel channel = plugin.getTransceiverHandler().getChannel(player);
                    if(channel != null) {
                        GUI.openWhoIsHereGUI(player, channel);
                    }
                    break;
                case 15: // 切断
                    player.closeInventory();
                    plugin.getTransceiverHandler().leaveFrequency(player);
                    break;
            }
        }
        else if (title.equals(GUI.WHO_IS_HERE_TITLE)) {
            event.setCancelled(true);
        }
    }

    private void sendShareRequest(Player sharer, Player target) {
        shareRequests.put(target.getUniqueId(), sharer.getUniqueId());
        sharer.sendMessage(plugin.getConfigManager().getMessage("share-request-sent").replace("%player%", target.getName()));
        TextComponent message = new TextComponent(plugin.getConfigManager().getMessage("share-request-received").replace("%player%", sharer.getName()));
        TextComponent acceptButton = new TextComponent(plugin.getConfigManager().getMessage("share-accept-button"));
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dp shareaccept"));
        message.addExtra(" ");
        message.addExtra(acceptButton);
        target.spigot().sendMessage(message);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (shareRequests.containsKey(target.getUniqueId()) && shareRequests.get(target.getUniqueId()).equals(sharer.getUniqueId())) {
                shareRequests.remove(target.getUniqueId());
                if(sharer.isOnline()) {
                    sharer.sendMessage(plugin.getConfigManager().getMessage("share-request-expired").replace("%player%", target.getName()));
                }
            }
        }, 20L * 30);
    }

    public Map<UUID, UUID> getShareRequests() {
        return shareRequests;
    }
}