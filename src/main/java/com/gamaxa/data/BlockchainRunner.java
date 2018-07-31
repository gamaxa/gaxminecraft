package com.gamaxa.data;

import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
        List<Transaction> transactions = ImmutableList.copyOf(this.plugin.getTracker().getTransactions());
        for (Transaction transaction : transactions) {
            long diff = System.currentTimeMillis() - transaction.getTimestamp();
            if (!transaction.isConfirmed() && diff > 30 * 1000) {
                this.plugin.getTracker().removeTransaction(transaction);
                Player seller = Bukkit.getPlayer(transaction.getSeller());
                Player buyer = Bukkit.getPlayer(transaction.getBuyer());
                if (seller != null) {
                    this.plugin.getData().sendMessage(seller, "lang.timeout.seller");
                }
                if (buyer != null) {
                    this.plugin.getData().sendMessage(buyer, "lang.timeout.buyer");
                }
                this.plugin.getTracker().givePlayerItem(transaction.getSeller(), transaction.getItemStack());
            } else if (transaction.isConfirmed() && diff > 10 * 60 * 1000) {
                this.plugin.getTracker().removeTransaction(transaction);
                Player seller = Bukkit.getPlayer(transaction.getSeller());
                Player buyer = Bukkit.getPlayer(transaction.getBuyer());
                if (seller != null) {
                    this.plugin.getData().sendMessage(seller, "lang.timeout.confirmed.seller");
                }
                if (buyer != null) {
                    this.plugin.getData().sendMessage(buyer, "lang.timeout.confirmed.buyer");
                }
                this.plugin.getTracker().givePlayerItem(transaction.getSeller(), transaction.getItemStack());
            }
        }
    }
}
