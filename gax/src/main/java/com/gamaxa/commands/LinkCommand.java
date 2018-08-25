package com.gamaxa.commands;

import com.gamaxa.GAXBukkit;
import com.google.common.collect.ImmutableMap;
import com.wavesplatform.wavesj.Base58;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class LinkCommand implements CommandExecutor {
    private final GAXBukkit plugin;

    public LinkCommand(GAXBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(this.plugin.getConfig().getString("lang.cmd.ingame"));
            return true;
        }
        Player player = (Player) commandSender;
        if (args.length == 1) {
            try {
                if (Base58.decode(args[0]).length != 26) {
                    throw new Throwable();
                }
            } catch (Throwable e) {
                this.plugin.getData().sendMessage(player, "lang.link.invalid");
                return true;
            }

            this.plugin.getStorage().setWavesAddress(player.getUniqueId(), args[0], e -> {
                if (e == null) {
                    this.plugin.getData().sendMessage(player, "lang.link.linked");
                } else {
                    this.plugin.getData().sendMessage(player, "lang.link.error");
                    this.plugin.getLogger().log(Level.WARNING, "Failed to link waves address", e);
                }
            });
        } else {
            this.plugin.getStorage().getWavesAddress(player.getUniqueId(), (addr, e) -> {
                if (e == null) {
                    if (addr == null) {
                        this.plugin.getData().sendMessage(player, "lang.link.unlinked");
                    } else {
                        this.plugin.getData().sendMessage(player, "lang.link.address", ImmutableMap.of(
                                "address", addr
                        ));
                    }
                    this.plugin.getData().sendMessage(player, "lang.link.guide");
                } else {
                    this.plugin.getData().sendMessage(player, "lang.link.error");
                    this.plugin.getLogger().log(Level.WARNING, "Failed to lookup waves address", e);
                }
            });
        }
        return true;
    }
}
