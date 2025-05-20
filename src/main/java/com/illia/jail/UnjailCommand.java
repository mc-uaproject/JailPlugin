package com.illia.jail;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class UnjailCommand implements CommandExecutor {
    private final JailPlugin plugin;

    public UnjailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jail.use")) {
            sender.sendMessage("§cНемає прав.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§eВикористання: /unjail <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cГравця не знайдено.");
            return true;
        }
        UUID uuid = target.getUniqueId();
        FileConfiguration cfg = plugin.getJailConfig();
        String base = "players." + uuid;
        if (!cfg.contains(base)) {
            sender.sendMessage("§cГравець не у в'язниці.");
            return true;
        }
        plugin.unjailPlayer(uuid);
        sender.sendMessage("§aГравця " + args[0] + " було випущено на волю.");
        return true;
    }
}
