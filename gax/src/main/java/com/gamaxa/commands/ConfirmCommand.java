package com.gamaxa.commands;

import com.gamaxa.Data;
import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class ConfirmCommand implements CommandExecutor {

    private final GAXBukkit plugin;

    public ConfirmCommand(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(this.plugin.getConfig().getString("lang.cmd.ingame"));
            return true;
        }
        Player player = (Player) commandSender;
        if (!this.plugin.getTradeTracker().isBuying(player.getUniqueId())) {
            this.plugin.getData().sendMessage(player, "lang.confirm.no_trades");
            return true;
        }
        this.plugin.getTradeTracker().confirm(player.getUniqueId(), (transaction, e) -> {
            if (e != null) {
                this.plugin.getData().sendMessage(player, "lang.confirm.error");
                this.plugin.getLogger().log(Level.WARNING, "Failed to confirm trade", e);
                return;
            }
            this.plugin.getData().sendMessage(player, "lang.confirm.confirmed", ImmutableMap.of(
                    "link", Data.getUrl(transaction.getAmount() + "", transaction.getSellerAddress()),
                    "code", transaction.getId()
            ));
            Player seller = Bukkit.getPlayer(transaction.getSeller());
            if (seller != null) {
                this.plugin.getData().sendMessage(seller, "lang.confirm.other_confirmed", ImmutableMap.of(
                        "player", player.getName()
                ));
            }
        });
        return true;
    }
}
