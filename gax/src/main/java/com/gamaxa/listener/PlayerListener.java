package com.gamaxa.listener;

import com.gamaxa.GAXBukkit;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class PlayerListener implements Listener {

    private final GAXBukkit plugin;

    public PlayerListener(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.getStorage().getPendingItems(player.getUniqueId(), (items, e) -> {
            if (e != null) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to lookup player's pending items", e);
                return;
            }
            if (!items.isEmpty()) {
                this.plugin.getData().sendMessage(player, "lang.claim.ready", ImmutableMap.of(
                        "items", items.size() + ""
                ));
            }
        });
    }
}
