package com.gamaxa.data;

import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * @author PaulBGD
 */
public class BlockchainRunner implements Runnable {

    private final GAXBukkit plugin;

    public BlockchainRunner(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<Transaction<ItemStack>> transactions = ImmutableList.copyOf(this.plugin.getTradeTracker().getTransactions());
        for (Transaction<ItemStack> transaction : transactions) {
            long diff = System.currentTimeMillis() - transaction.getTimestamp();
            if (!transaction.isConfirmed() && diff > 30 * 1000) {
                this.plugin.getTradeTracker().removeTransaction(transaction);
                Player seller = Bukkit.getPlayer(transaction.getSeller());
                Player buyer = Bukkit.getPlayer(transaction.getBuyer());
                if (seller != null) {
                    this.plugin.getData().sendMessage(seller, "lang.timeout.seller");
                }
                if (buyer != null) {
                    this.plugin.getData().sendMessage(buyer, "lang.timeout.buyer");
                }
                this.plugin.getTradeTracker().givePlayerItem(transaction.getSeller(), transaction.getItem());
            } else if (transaction.isConfirmed() && diff > 10 * 60 * 1000) {
                this.plugin.getTradeTracker().removeTransaction(transaction);
                Player seller = Bukkit.getPlayer(transaction.getSeller());
                Player buyer = Bukkit.getPlayer(transaction.getBuyer());
                if (seller != null) {
                    this.plugin.getData().sendMessage(seller, "lang.timeout.confirmed.seller");
                }
                if (buyer != null) {
                    this.plugin.getData().sendMessage(buyer, "lang.timeout.confirmed.buyer");
                }
                this.plugin.getTradeTracker().givePlayerItem(transaction.getSeller(), transaction.getItem());
            }
        }
    }
}
