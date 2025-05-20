package com.illia.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class JailPlugin extends JavaPlugin implements Listener {
    private File jailFile;
    private FileConfiguration jailConfig;

    @Override
    public void onEnable() {
        this.getCommand("jail").setExecutor(new JailCommand(this));
        this.getCommand("unjail").setExecutor(new UnjailCommand(this));
        Bukkit.getPluginManager().registerEvents(this, this);

        jailFile = new File(getDataFolder(), "jails.yml");
        if (!jailFile.exists()) {
            getDataFolder().mkdirs();
            saveResource("jails.yml", false);
        }
        jailConfig = YamlConfiguration.loadConfiguration(jailFile);

        new BukkitRunnable() {
            @Override
            public void run() {
                checkReleases();
            }
        }.runTaskTimer(this, 1200L, 1200L);
    }

    @Override
    public void onDisable() {
        saveJailConfig();
    }

    public FileConfiguration getJailConfig() {
        return jailConfig;
    }

    public void saveJailConfig() {
        try {
            jailConfig.save(jailFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save jails.yml", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String path = "players." + player.getUniqueId();
        if (jailConfig.contains(path)) {
            Action action = event.getAction();
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK
                    || action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                player.sendActionBar("§cВи наразі у в'язниці і не можете використовувати кліки.");
            }
        }
    }

    // Prevent jailed players dealing damage
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            String path = "players." + damager.getUniqueId();
            if (jailConfig.contains(path)) {
                event.setCancelled(true);
                damager.sendActionBar("§cУ вас немає права завдавати шкоди під час ув'язнення.");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String path = "players." + player.getUniqueId();
        if (jailConfig.contains(path)) {
            double x = jailConfig.getDouble(path + ".jail.x");
            double y = jailConfig.getDouble(path + ".jail.y");
            double z = jailConfig.getDouble(path + ".jail.z");
            String world = jailConfig.getString(path + ".jail.world");
            Location jailLoc = new Location(Bukkit.getWorld(world), x, y, z);
            player.teleport(jailLoc);
            player.setGameMode(GameMode.ADVENTURE);
            player.setBedSpawnLocation(jailLoc, true);
        }
    }

    public void unjailPlayer(UUID uuid) {
        String base = "players." + uuid;
        if (!jailConfig.contains(base)) return;

        Player player = Bukkit.getPlayer(uuid);
        double ox = jailConfig.getDouble(base + ".original.x");
        double oy = jailConfig.getDouble(base + ".original.y");
        double oz = jailConfig.getDouble(base + ".original.z");
        float oyaw = (float) jailConfig.getDouble(base + ".original.yaw");
        float opitch = (float) jailConfig.getDouble(base + ".original.pitch");
        String oworld = jailConfig.getString(base + ".original.world");
        Location origLoc = new Location(Bukkit.getWorld(oworld), ox, oy, oz, oyaw, opitch);

        String spawnWorld = jailConfig.getString(base + ".spawn.world");
        int sx = jailConfig.getInt(base + ".spawn.x");
        int sy = jailConfig.getInt(base + ".spawn.y");
        int sz = jailConfig.getInt(base + ".spawn.z");
        Location spawnLoc = new Location(Bukkit.getWorld(spawnWorld), sx, sy, sz);

        GameMode gm = GameMode.valueOf(jailConfig.getString(base + ".gamemode"));

        if (player != null && player.isOnline()) {
            player.teleport(origLoc);
            player.setBedSpawnLocation(spawnLoc, true);
            player.setGameMode(gm);
            player.sendMessage("§aВас звільнено з в'язниці.");
        }
        jailConfig.set(base, null);
        saveJailConfig();
    }

    private void checkReleases() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        if (!jailConfig.contains("players")) return;
        Set<String> uuids = new HashSet<>(jailConfig.getConfigurationSection("players").getKeys(false));
        for (String uuid : uuids) {
            String base = "players." + uuid;
            long release = jailConfig.getLong(base + ".releaseTime");
            if (now >= release) {
                unjailPlayer(UUID.fromString(uuid));
                changed = true;
            }
        }
        if (changed) saveJailConfig();
    }
}