package com.illia.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class JailCommand implements CommandExecutor {
    private final JailPlugin plugin;

    public JailCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jail.use")) {
            sender.sendMessage("§cНемає прав");
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage("§eВикористання: /jail <player> <days> <x> <y> <z>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage("§cГравця не знайдено.");
            return true;
        }
        long days;
        try { days = Long.parseLong(args[1]); } catch (NumberFormatException e) {
            sender.sendMessage("§cНекоректна кількість днів."); return true;
        }
        double x, y, z;
        try { x = Double.parseDouble(args[2]); y = Double.parseDouble(args[3]); z = Double.parseDouble(args[4]); }
        catch (NumberFormatException e) { sender.sendMessage("§cНекоректні координати."); return true; }

        UUID uuid = target.getUniqueId();
        FileConfiguration cfg = plugin.getJailConfig();
        String path = "players." + uuid;

        Player online = Bukkit.getPlayer(uuid);
        Location original = null;
        Location spawn = null;
        GameMode originalGM = null;
        if (online != null && online.isOnline()) {
            original = online.getLocation();
            spawn = online.getBedSpawnLocation();
            originalGM = online.getGameMode();
        }

        long releaseTime = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L;

        if (original != null) {
            cfg.set(path + ".original.world", original.getWorld().getName());
            cfg.set(path + ".original.x", original.getX());
            cfg.set(path + ".original.y", original.getY());
            cfg.set(path + ".original.z", original.getZ());
            cfg.set(path + ".original.yaw", original.getYaw());
            cfg.set(path + ".original.pitch", original.getPitch());
        }
        if (spawn != null) {
            cfg.set(path + ".spawn.world", spawn.getWorld().getName());
            cfg.set(path + ".spawn.x", spawn.getBlockX());
            cfg.set(path + ".spawn.y", spawn.getBlockY());
            cfg.set(path + ".spawn.z", spawn.getBlockZ());
        }
        if (originalGM != null) cfg.set(path + ".gamemode", originalGM.name());
        cfg.set(path + ".jail.world", Bukkit.getWorlds().get(0).getName());
        cfg.set(path + ".jail.x", x);
        cfg.set(path + ".jail.y", y);
        cfg.set(path + ".jail.z", z);
        cfg.set(path + ".releaseTime", releaseTime);

        plugin.saveJailConfig();

        if (online != null && online.isOnline()) {
            Location jailLoc = new Location(Bukkit.getWorlds().get(0), x, y, z);
            online.teleport(jailLoc);
            online.setBedSpawnLocation(jailLoc, true);
            online.setGameMode(GameMode.ADVENTURE);
        }

        sender.sendMessage("§aPlayer " + args[0] + " jailed for " + days + " days.");
        return true;
    }
}

