package com.gamaxa.commands;

import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class ClaimCommand implements CommandExecutor {

    private final GAXBukkit plugin;

    public ClaimCommand(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(this.plugin.getConfig().getString("lang.cmd.ingame"));
            return true;
        }
        Player player = (Player) commandSender;
        this.plugin.getStorage().getPendingItems(player.getUniqueId(), (items, e) -> {
            if (e != null) {
                this.plugin.getData().sendMessage(player, "lang.claim.error");
                this.plugin.getLogger().log(Level.WARNING, "Failed to get pending items", e);
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            this.plugin.getStorage().clearPendingItems(player.getUniqueId());
            this.plugin.getData().sendMessage(player, "lang.claim.claimed", ImmutableMap.of(
                    "items", ((Integer) items.size()).toString()
            ));
            HashMap<Integer, ItemStack> map = player.getInventory().addItem(items.toArray(new ItemStack[0]));
            for (ItemStack itemStack : map.values()) {
                player.getWorld().dropItem(player.getLocation(), itemStack);
            }
            this.plugin.getStorage().clearPendingItems(player.getUniqueId());
        });
        return true;
    }
}
