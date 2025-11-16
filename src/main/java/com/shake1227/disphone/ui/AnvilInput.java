package com.shake1227.disphone.ui;

import com.shake1227.disphone.DisPhone;
import com.shake1227.disphone.handler.CallHandler;
import com.shake1227.disphone.manager.ConfigManager;
import com.shake1227.disphone.manager.DataManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.UUID;
import java.util.regex.Pattern;

public class AnvilInput {

    private static final DisPhone plugin = DisPhone.getInstance();
    private static final DataManager dataManager = plugin.getDataManager();
    private static final ConfigManager configManager = plugin.getConfigManager();
    private static final CallHandler callHandler = plugin.getCallHandler();
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]{8}$");

    public static void openRegistrationGUI(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(configManager.getMessage("anvil.register.title"))
                .text(configManager.getMessage("anvil.register.text"))
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String input = stateSnapshot.getText();
                    if (NUMBER_PATTERN.matcher(input).matches()) {
                        if (dataManager.phoneNumberExists(input)) {
                            player.sendMessage(configManager.getMessage("number-taken"));
                        } else {
                            dataManager.setPhoneNumber(player.getUniqueId(), input);
                            player.sendMessage(configManager.getMessage("register-success")
                                    .replace("%number%", dataManager.getPhoneNumber(player.getUniqueId())));
                        }
                    } else {
                        player.sendMessage(configManager.getMessage("invalid-number-format"));
                    }
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }
    public static void openCallGUI(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(configManager.getMessage("anvil.call.title"))
                .text(configManager.getMessage("anvil.call.text"))
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String input = stateSnapshot.getText();
                    if (isValidPhoneNumber(input)) {
                        UUID targetUUID = dataManager.getPlayerByPhoneNumber(input);
                        if (targetUUID != null) {
                            Player target = Bukkit.getPlayer(targetUUID);
                            if (target != null && target.isOnline()) {
                                callHandler.startCall(player, target);
                            } else {
                                player.sendMessage(configManager.getMessage("player-not-found-offline"));
                            }
                        } else {
                            player.sendMessage(configManager.getMessage("player-not-found"));
                        }
                    } else {
                        player.sendMessage(configManager.getMessage("invalid-call-format"));
                    }
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }
    public static void openInviteGUI(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(configManager.getMessage("anvil.invite.title"))
                .text(configManager.getMessage("anvil.invite.text"))
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String input = stateSnapshot.getText();
                    if (isValidPhoneNumber(input)) {
                        UUID targetUUID = dataManager.getPlayerByPhoneNumber(input);
                        if (targetUUID != null) {
                            Player target = Bukkit.getPlayer(targetUUID);
                            if (target != null && target.isOnline()) {
                                callHandler.inviteToCall(player, target);
                            } else {
                                player.sendMessage(configManager.getMessage("player-not-found-offline"));
                            }
                        } else {
                            player.sendMessage(configManager.getMessage("player-not-found"));
                        }
                    } else {
                        player.sendMessage(configManager.getMessage("invalid-call-format"));
                    }
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }
    public static void openFrequencyGUI(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(configManager.getMessage("transceiver.anvil.title"))
                .text(configManager.getMessage("transceiver.anvil.text"))
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    String input = stateSnapshot.getText();
                    if (input.matches("^[0-9]+$")) {
                        plugin.getTransceiverHandler().joinFrequency(player, input);
                    } else {
                        player.sendMessage(configManager.getMessage("transceiver.invalid-frequency"));
                    }
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    private static boolean isValidPhoneNumber(String input) {
        String formatted = input.replace("-", "");
        return formatted.length() == 11 && formatted.startsWith("020");
    }
}