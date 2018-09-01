package com.gamaxa.commands;

import com.gamaxa.Data;
import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class GiftcardCommand implements CommandExecutor {
    private final GAXBukkit plugin;

    public GiftcardCommand(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(this.plugin.getConfig().getString("lang.cmd.ingame"));
            return true;
        }
        Player player = (Player) commandSender;
        this.plugin.gettTracker().createTransaction(player.getUniqueId(), (t, e) -> {
            if (e != null) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to create transaction", e);
                this.plugin.getData().sendMessage(player, "lang.confirm.error");
                return;
            }
            this.plugin.getData().sendMessage(player, "lang.giftcard.created", ImmutableMap.of(
                    "giftcard", t.getItem().code,
                    "curr", this.plugin.getConfig().getString("buycraft.ratio"),
                    "link", Data.getUrl("", this.plugin.getConfig().getString("buycraft.address")),
                    "code", t.getId()
            ));
        });
        return false;
    }
}
