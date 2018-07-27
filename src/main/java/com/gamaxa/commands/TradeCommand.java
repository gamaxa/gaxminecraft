package com.gamaxa.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TradeCommand implements CommandExecutor {
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("You must be in-game to use this command.");
            return true;
        }

        Player player = (Player) commandSender;
        if (args.length != 2) {
            player.sendMessage("Usage: /gaxtrade <player> <amount>");
            return true;
        }
        Player other = Bukkit.getPlayer(args[0]);
        if (other == null) {
            player.sendMessage("The player you're trying to trade with is offline.");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid amount to trade for.");
            return true;
        }

        return true;
    }
}
