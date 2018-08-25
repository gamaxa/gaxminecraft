package com.gamaxa.storage;

import com.gamaxa.GAXBukkit;
import com.gamaxa.utils.JSONChat;
import com.google.common.collect.ImmutableMap;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author PaulBGD
 */
public class Configuration {

    private final GAXBukkit plugin;
    private final FileConfiguration config;
    private boolean loggedDebug = false;

    private void updateSection(ConfigurationSection configSec, ConfigurationSection builtinSec) {
        for (String key : builtinSec.getKeys(true)) {
            System.out.println("Contains " + builtinSec.getCurrentPath() + "." + key + " " + configSec.contains(key));
            if (!configSec.contains(key)) {
                configSec.set(key, builtinSec.get(key));
            }
        }
    }

    public Configuration(GAXBukkit plugin) {
        this.plugin = plugin;

        this.plugin.saveDefaultConfig();
        this.config = YamlConfiguration.loadConfiguration(new File(this.plugin.getDataFolder(), "config.yml"));

        YamlConfiguration builtin = YamlConfiguration.loadConfiguration(new InputStreamReader(this.plugin.getResource("config.yml")));
        if (this.config.getInt("version", 0) < builtin.getInt("version")) {
            updateSection(this.config, builtin);
            this.config.set("version", builtin.getInt("version"));
            try {
                this.config.save(new File(this.plugin.getDataFolder(), "config.yml"));
                this.plugin.reloadConfig();
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to save config.yml changes!", e);
            }
        }
    }

    private String match(String original, Map<String, String> data) {
        original = ChatColor.translateAlternateColorCodes('&', original);
        for (Map.Entry<String, String> entry : data.entrySet()) {
            original = original.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return original;
    }

    public void sendMessage(Player player, String path) {
        this.sendMessage(player, path, ImmutableMap.of(), null);
    }

    public void sendMessage(Player player, String path, Map<String, String> data) {
        this.sendMessage(player, path, data, null);
    }

    public void sendMessage(Player player, String path, Map<String, String> data, ItemStack itemStack) {
        if (this.config.isList(path)) {
            for (String s : this.config.getStringList(path)) {
                player.sendMessage(match(s, data));
            }
        } else if (this.config.isConfigurationSection(path)) {
            ConfigurationSection section = this.config.getConfigurationSection(path);
            String text = section.getString("text");
            TextComponent component = new TextComponent(match(text, data));
            component.setColor(net.md_5.bungee.api.ChatColor.valueOf(section.getString("color", "WHITE")));
            if (itemStack != null) {
                try {
                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{JSONChat.serializeItemStack(itemStack)}));
                } catch (ReflectiveOperationException e) {
                    if (!this.loggedDebug) { // only log once per restart
                        this.loggedDebug = true;
                        this.plugin.getLogger().log(Level.WARNING, "Failed to serialize itemstack", e);
                    }
                }
            }
            player.spigot().sendMessage(component);
        } else {
            player.sendMessage(match(this.config.getString(path), data));
        }
    }
}
