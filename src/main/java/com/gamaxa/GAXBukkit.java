package com.gamaxa;

import com.gamaxa.commands.ClaimCommand;
import com.gamaxa.commands.ConfirmCommand;
import com.gamaxa.commands.LinkCommand;
import com.gamaxa.commands.TradeCommand;
import com.gamaxa.data.BlockchainRunner;
import com.gamaxa.data.Tracker;
import com.gamaxa.listener.PlayerListener;
import com.gamaxa.storage.Configuration;
import com.gamaxa.storage.FlatStorage;
import com.gamaxa.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author PaulBGD
 */
public class GAXBukkit extends JavaPlugin {

    private Configuration configuration;
    private Storage storage;
    private Tracker tracker;

    @Override
    public void onEnable() {
        this.configuration = new Configuration(this);
        this.storage = new FlatStorage(this);

        getCommand("gaxtrade").setExecutor(new TradeCommand(this));
        getCommand("gaxlink").setExecutor(new LinkCommand(this));
        getCommand("gaxconfirm").setExecutor(new ConfirmCommand(this));
        getCommand("gaxclaim").setExecutor(new ClaimCommand(this));

        try {
            this.tracker = new Tracker(this, this.storage.getAndClearTransactions());
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, new BlockchainRunner(this), 5 * 20L, 5 * 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.tracker, 10 * 20L, 10 * 20L);
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
}
