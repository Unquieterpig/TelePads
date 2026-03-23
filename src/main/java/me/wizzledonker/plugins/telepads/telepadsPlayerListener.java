package me.wizzledonker.plugins.telepads;

import java.util.HashSet;
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

public class telepadsPlayerListener implements Listener{
    public static Telepads plugin;
    
    private Set<UUID> onPad = new HashSet<>();
    
    public telepadsPlayerListener(Telepads instance) {
        plugin = instance;
    }
    
    @EventHandler
    public void whenPlayerMoves(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        
        final Location from = event.getFrom();
        final Location to = event.getTo();
        
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getBlockY() == to.getBlockY()) {
            return;
        }
        
        if (onPad.contains(player.getUniqueId())) return;
        if (player.hasPermission("telepads.use")) {
            Block block = to.getBlock().getRelative(BlockFace.DOWN);
            final Location loc = new Location(null, block.getX(), block.getY(), block.getZ());
            if (!checkPad(block, loc)) {
                return;
            }
            onPad.add(player.getUniqueId());
            player.sendMessage(ChatColor.GRAY + plugin.wait_msg.replace("%time%", plugin.telepad_teleport_time + " Seconds"));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                onPad.remove(player.getUniqueId());
                Block cBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                Location fLoc = new Location(null, cBlock.getX(), cBlock.getY(), cBlock.getZ());
                if (!checkPad(cBlock, fLoc)) return;
                if (!java.util.Objects.equals(plugin.telepads.get(loc), plugin.telepads.get(fLoc))) return;
                plugin.gotoPad(loc, player);
            }, plugin.telepad_teleport_time * 20L);
        }
    }
    
    @EventHandler
    public void whenPlayerBreaksBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("telepads.create")) {
            return;
        }
        Block block = event.getBlock();
        final Location loc = new Location(null, block.getX(), block.getY(), block.getZ());
        if (!checkPad(block, loc)) {
            return;
        }
        player.sendMessage(ChatColor.RED + "You're not allowed to break that pad!");
        event.setCancelled(true);
    }
    
    private boolean checkPad(Block block, Location loc) {
        if (block.getType() != plugin.telepad_material) {
            return false;
        }
        if (!plugin.telepads.containsKey(loc)) {
            return false;
        }
        return true;
    }
    
}
