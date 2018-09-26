package com.gamaxa.commands;

import com.gamaxa.GAXBukkit;
import com.gamaxa.data.Transaction;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.logging.Level;

public class TradeCommand implements CommandExecutor {

    private final GAXBukkit plugin;
    private final SecureRandom secureRandom = new SecureRandom();

    public TradeCommand(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(this.plugin.getConfig().getString("lang.cmd.ingame"));
            return true;
        }

        Player player = (Player) commandSender;
        if (args.length != 2) {
            this.plugin.getData().sendMessage(player, "lang.trade.usage");
            return true;
        }
        Player other = Bukkit.getPlayer(args[0]);
        if (other == null) {
            this.plugin.getData().sendMessage(player, "lang.trade.offline");
            return true;
        }
        if (player == other) {
            this.plugin.getData().sendMessage(player, "lang.trade.self");
            return true;
        }
        if (this.plugin.getTradeTracker().isBuying(other.getUniqueId())) {
            this.plugin.getData().sendMessage(player, "lang.trade.existing_trade");
            return true;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(args[1]);
        } catch (NumberFormatException e) {
            this.plugin.getData().sendMessage(player, "lang.trade.invalid_amount");
            return true;
        }
        if (amount.scale() > 8) {
            this.plugin.getData().sendMessage(player, "lang.trade.invalid_amount");
            return true;
        }

        this.plugin.getStorage().getWavesAddress(player.getUniqueId(), (addr, e) -> {
            if (e != null) {
                this.plugin.getData().sendMessage(player, "lang.trade.error_lookup");
                this.plugin.getLogger().log(Level.WARNING, "Failed to lookup Waves Address", e);
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            if (!other.isOnline()) {
                this.plugin.getData().sendMessage(player, "lang.trade.went_offline");
                return;
            }
            if (addr == null) {
                this.plugin.getData().sendMessage(player, "lang.trade.unlinked");
                return;
            }

            if (this.plugin.getTradeTracker().isSelling(addr)) {
                this.plugin.getData().sendMessage(player, "lang.trade.already");
                return;
            }

            if (player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR) {
                this.plugin.getData().sendMessage(player, "lang.trade.no_item");
                return;
            }

            int rand = this.secureRandom.nextInt(100000);
            Transaction<ItemStack> trans = new Transaction<>(String.format("%04d", rand), player.getUniqueId(), other.getUniqueId(), player.getItemInHand().clone(), amount, addr);
            player.setItemInHand(null);
            this.plugin.getStorage().saveTransaction(trans, e2 -> {
                if (e2 != null) {
                    this.plugin.getData().sendMessage(player, "lang.trade.start_failed");
                    this.plugin.getTradeTracker().givePlayerItem(player.getUniqueId(), trans.getItem());
                    this.plugin.getLogger().log(Level.WARNING, "Failed to save transaction", e2);
                    return;
                }
                this.plugin.getTradeTracker().addTransaction(trans);
                this.plugin.getData().sendMessage(player, "lang.trade.waiting");
                this.plugin.getData().sendMessage(other, "lang.trade.received", ImmutableMap.of(
                        "player", player.getName(),
                        "amount", amount + ""
                ));
                this.plugin.getData().sendMessage(player, "lang.trade.received_itemstack", ImmutableMap.of(
                        "item", trans.getItem().getType().name().toLowerCase(),
                        "amount", trans.getItem().getAmount() + ""
                ), trans.getItem());
            });
        });

        return true;
    }
}
