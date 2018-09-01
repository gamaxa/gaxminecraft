package com.gamaxa;

import com.gamaxa.buycraft.BuycraftGiftcard;
import com.gamaxa.buycraft.TransactionTracker;
import com.gamaxa.commands.*;
import com.gamaxa.data.BlockchainRunner;
import com.gamaxa.data.Tracker;
import com.gamaxa.data.Transaction;
import com.gamaxa.listener.PlayerListener;
import com.gamaxa.storage.Configuration;
import com.gamaxa.storage.FlatStorage;
import com.gamaxa.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author PaulBGD
 */
public class GAXBukkit extends JavaPlugin {

    private Configuration configuration;
    private Storage storage;
    private Tracker tracker;
    private TransactionTracker tTracker;

    @Override
    public void onEnable() {
        this.configuration = new Configuration(this);
        this.storage = new FlatStorage(this);

        getCommand("gaxtrade").setExecutor(new TradeCommand(this));
        getCommand("gaxlink").setExecutor(new LinkCommand(this));
        getCommand("gaxconfirm").setExecutor(new ConfirmCommand(this));
        getCommand("gaxclaim").setExecutor(new ClaimCommand(this));

        List<Transaction<?>> transactions = this.storage.getAndClearTransactions();
        List<Transaction<ItemStack>> trades = transactions.stream()
                .filter(t -> t.getItem() instanceof ItemStack)
                .map(t -> (Transaction<ItemStack>) t)
                .collect(Collectors.toList());
        List<Transaction<BuycraftGiftcard>> giftcards = transactions.stream()
                .filter(t -> t.getItem() instanceof BuycraftGiftcard)
                .map(t -> (Transaction<BuycraftGiftcard>) t)
                .collect(Collectors.toList());

        try {
            this.tracker = new Tracker(this, trades);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, new BlockchainRunner(this), 5 * 20L, 5 * 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.tracker, 10 * 20L, 15 * 20L);

        if (this.getConfig().getBoolean("buycraft.enabled")) {
            getCommand("gaxcard").setExecutor(new GiftcardCommand(this));
            try {
                this.tTracker = new TransactionTracker(this, giftcards);
                Bukkit.getScheduler().runTaskTimerAsynchronously(this, tTracker, 20L, 5 * 20L);
            } catch (URISyntaxException e) {
                this.getLogger().log(Level.WARNING, "", e);
            }
        }
    }

    public Configuration getData() {
        return configuration;
    }

    public Storage getStorage() {
        return storage;
    }

    public Tracker getTracker() {
        return tracker;
    }

    public TransactionTracker gettTracker() {
        return tTracker;
    }
}
