package me.wizzledonker.plugins.telepads;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TelepadPlayerListener implements Listener {

    private final Telepads plugin;
    private final Set<UUID> playersOnPad = new HashSet<>();

    public TelepadPlayerListener(Telepads plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (playersOnPad.contains(player.getUniqueId())) return;
        if (!player.hasPermission("telepads.use")) return;

        Block block = to.getBlock().getRelative(BlockFace.DOWN);
        if (block.getType() != plugin.getTelepadMaterial()) return;

        Location padLoc = new Location(null, block.getX(), block.getY(), block.getZ());
        if (!plugin.hasPadAt(padLoc)) return;

        playersOnPad.add(player.getUniqueId());
        int teleportTime = plugin.getTeleportTime();
        player.sendMessage(ChatColor.GRAY + plugin.getWaitMessage()
                .replace("%time%", teleportTime + " Seconds"));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            playersOnPad.remove(player.getUniqueId());
            if (!player.isOnline()) return;

            Block currentBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (currentBlock.getType() != plugin.getTelepadMaterial()) return;

            Location currentLoc = new Location(null, currentBlock.getX(), currentBlock.getY(), currentBlock.getZ());
            if (!plugin.hasPadAt(currentLoc)) return;
            if (!Objects.equals(plugin.getPadName(padLoc), plugin.getPadName(currentLoc))) return;

            plugin.gotoPad(padLoc, player);
        }, teleportTime * 20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("telepads.create")) return;

        Block block = event.getBlock();
        if (block.getType() != plugin.getTelepadMaterial()) return;

        Location loc = new Location(null, block.getX(), block.getY(), block.getZ());
        if (!plugin.hasPadAt(loc)) return;

        player.sendMessage(ChatColor.RED + "You're not allowed to break that pad!");
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playersOnPad.remove(event.getPlayer().getUniqueId());
    }
}
