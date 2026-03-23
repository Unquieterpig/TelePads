/*
 * TELEPADS - By WizzleDonker
 * Originally made for LordOfJustice
 */
package me.wizzledonker.plugins.telepads;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import me.wizzledonker.plugins.telepads.config.PadConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Telepads extends JavaPlugin {

    private static final Pattern VALID_PAD_NAME = Pattern.compile("^[a-zA-Z0-9_\\-]{1,32}$");
    private static final String LOCATION_DEST_MARKER = "@location";

    private final Map<Location, String> padLocations = new HashMap<>();
    private final Map<String, String> padLinks = new HashMap<>();

    private PadConfiguration padConfiguration;
    private FileConfiguration padConfig;

    private Material telepadMaterial = Material.STONE;
    private int teleportTime = 3;
    private boolean noDestMessageEnabled = true;
    private String teleportMessage = "Successfully teleported to %pad%";
    private String teleportDeniedMessage = "You are not allowed to use this pad!";
    private String waitMessage = "Teleporting in %time%, stay on the pad!";
    private boolean teleportMessageEnabled = true;

    public Material getTelepadMaterial() {
        return telepadMaterial;
    }

    public int getTeleportTime() {
        return teleportTime;
    }

    public String getWaitMessage() {
        return waitMessage;
    }

    public Map<String, String> getTeleLinks() {
        return Collections.unmodifiableMap(padLinks);
    }

    public boolean hasPadAt(Location loc) {
        return padLocations.containsKey(loc);
    }

    public String getPadName(Location loc) {
        return padLocations.get(loc);
    }

    @Override
    public void onDisable() {
        padConfiguration.savePadConfig();
        getLogger().info("TelePads is now disabled!");
    }

    @Override
    public void onEnable() {
        padConfiguration = new PadConfiguration(this);
        padConfig = padConfiguration.getPadConfig();

        getServer().getPluginManager().registerEvents(new TelepadPlayerListener(this), this);

        TelepadCommands commandExecutor = new TelepadCommands(this);
        getCommand("createpad").setExecutor(commandExecutor);
        getCommand("padlink").setExecutor(commandExecutor);
        getCommand("padunlink").setExecutor(commandExecutor);
        getCommand("delpad").setExecutor(commandExecutor);
        getCommand("linkpadhere").setExecutor(commandExecutor);
        getCommand("padlist").setExecutor(commandExecutor);

        reloadProperties();
        getLogger().info("TelePads by wizzledonker is now enabled!");
    }

    private boolean isValidPadName(String name) {
        return name != null && VALID_PAD_NAME.matcher(name).matches();
    }

    public void createPad(Player player, String name) {
        if (!player.hasPermission("telepads.create")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to make a teleport pad!");
            return;
        }
        if (!isValidPadName(name)) {
            player.sendMessage(ChatColor.RED + "Invalid pad name! Use only letters, numbers, underscores, and hyphens (max 32 chars).");
            return;
        }
        if (padLinks.containsKey(name)) {
            player.sendMessage(ChatColor.RED + "This pad already exists! Remove it before creating a new one.");
            return;
        }
        Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (block.getType() != telepadMaterial) {
            player.sendMessage(ChatColor.RED + "The block below you is not of the right type!");
            return;
        }

        padConfig.set("pads." + name + ".X", block.getX());
        padConfig.set("pads." + name + ".Y", block.getY());
        padConfig.set("pads." + name + ".Z", block.getZ());
        padConfig.set("pads." + name + ".pitch", player.getLocation().getPitch());
        padConfig.set("pads." + name + ".yaw", player.getLocation().getYaw());
        padConfig.set("pads." + name + ".world", block.getLocation().getWorld().getName());
        padConfig.set("pads." + name + ".dest", null);
        padConfiguration.savePadConfig();
        reloadProperties();

        player.sendMessage(ChatColor.GREEN + "Successfully created teleport pad " + name);
    }

    public void deletePad(CommandSender sender, String name) {
        if (!sender.hasPermission("telepads.delete")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to delete this Teleport Pad!");
            return;
        }
        if (!padLinks.containsKey(name)) {
            sender.sendMessage(ChatColor.RED + "That pad does not exist!");
            return;
        }

        padConfig.set("pads." + name, null);
        padConfiguration.savePadConfig();
        getServer().getPluginManager().removePermission("telepads.pads." + name);
        reloadProperties();

        String msg = getConfig().getString("messages.delete", "Teleport pad %pad% removed.");
        sender.sendMessage(ChatColor.DARK_GREEN + msg.replace("%pad%", name));
    }

    public void linkPads(String pad1, String pad2, Player player) {
        if (!player.hasPermission("telepads.link")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to link these pads!");
            return;
        }
        if (!padLinks.containsKey(pad1) || !padLinks.containsKey(pad2)) {
            player.sendMessage(ChatColor.RED + "One of the pads doesn't exist!");
            return;
        }
        if (pad1.equals(pad2)) {
            player.sendMessage(ChatColor.RED + "You cannot link a pad to itself!");
            return;
        }

        padConfig.set("pads." + pad1 + ".dest", pad2);
        padConfig.set("pads." + pad2 + ".dest", pad1);
        padConfiguration.savePadConfig();
        reloadProperties();

        String msg = getConfig().getString("messages.link", "Successfully linked %pad% to %pad2%");
        player.sendMessage(ChatColor.GREEN + msg.replace("%pad%", pad1).replace("%pad2%", pad2));
    }

    public void unlinkPad(String pad, CommandSender sender) {
        if (!sender.hasPermission("telepads.unlink")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to unlink this Teleport Pad!");
            return;
        }
        if (!padLinks.containsKey(pad)) {
            sender.sendMessage(ChatColor.RED + "That pad does not exist!");
            return;
        }

        padConfig.set("pads." + pad + ".dest", null);
        padConfig.set("pads." + pad + ".dest_loc", null);
        padConfiguration.savePadConfig();
        reloadProperties();

        String msg = getConfig().getString("messages.unlink", "Set destination of %pad% to nowhere.");
        sender.sendMessage(ChatColor.GREEN + msg.replace("%pad%", pad));
    }

    public void linkPadHere(Player player, String padName) {
        if (!player.hasPermission("telepads.linkhere")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to link pads to locations!");
            return;
        }
        if (!padLinks.containsKey(padName)) {
            player.sendMessage(ChatColor.RED + "That pad does not exist!");
            return;
        }

        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        padConfig.set("pads." + padName + ".dest", LOCATION_DEST_MARKER);
        padConfig.set("pads." + padName + ".dest_loc.X", loc.getBlockX());
        padConfig.set("pads." + padName + ".dest_loc.Y", loc.getBlockY());
        padConfig.set("pads." + padName + ".dest_loc.Z", loc.getBlockZ());
        padConfig.set("pads." + padName + ".dest_loc.pitch", loc.getPitch());
        padConfig.set("pads." + padName + ".dest_loc.yaw", loc.getYaw());
        padConfig.set("pads." + padName + ".dest_loc.world", loc.getWorld().getName());
        padConfiguration.savePadConfig();
        reloadProperties();

        player.sendMessage(ChatColor.GREEN + "Linked pad " + padName + " to your current location!");
    }

    public void gotoPad(Location loc, Player player) {
        String padName = getPadName(loc);
        if (padName == null) return;

        if (!player.hasPermission("telepads.pads." + padName) && !player.hasPermission("telepads.pads")) {
            player.sendMessage(ChatColor.RED + teleportDeniedMessage);
            return;
        }

        String dest = padLinks.get(padName);
        if (dest == null || "nowhere".equals(dest)) {
            if (noDestMessageEnabled) {
                player.sendMessage("No destination!");
            }
            return;
        }

        Location tele;
        String destDisplayName;

        if (LOCATION_DEST_MARKER.equals(dest)) {
            tele = resolveLocationDest(padName);
            destDisplayName = padName + " (location)";
        } else {
            tele = resolvePadDest(dest);
            destDisplayName = dest;
        }

        if (tele == null || tele.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Destination world not found!");
            return;
        }

        if (tele.getBlock().getType() != Material.AIR) {
            tele.add(0, 2, 0);
        }

        player.teleport(tele);

        if (teleportMessageEnabled) {
            player.sendMessage(ChatColor.GREEN + teleportMessage.replace("%pad%",
                    ChatColor.WHITE + destDisplayName + ChatColor.GREEN));
        }
    }

    private Location resolveLocationDest(String padName) {
        String worldName = padConfig.getString("pads." + padName + ".dest_loc.world");
        World world = worldName != null ? getServer().getWorld(worldName) : null;
        if (world == null) return null;

        Location tele = new Location(world,
                padConfig.getInt("pads." + padName + ".dest_loc.X") + 0.5,
                padConfig.getInt("pads." + padName + ".dest_loc.Y") + 1,
                padConfig.getInt("pads." + padName + ".dest_loc.Z") + 0.5);
        tele.setPitch((float) padConfig.getDouble("pads." + padName + ".dest_loc.pitch"));
        tele.setYaw((float) padConfig.getDouble("pads." + padName + ".dest_loc.yaw"));
        return tele;
    }

    private Location resolvePadDest(String destPad) {
        String worldName = padConfig.getString("pads." + destPad + ".world");
        World world = worldName != null ? getServer().getWorld(worldName) : null;
        if (world == null) return null;

        Location tele = new Location(world,
                padConfig.getInt("pads." + destPad + ".X") + 0.5,
                padConfig.getInt("pads." + destPad + ".Y") + 1,
                padConfig.getInt("pads." + destPad + ".Z") + 0.5);
        tele.setPitch((float) padConfig.getDouble("pads." + destPad + ".pitch"));
        tele.setYaw((float) padConfig.getDouble("pads." + destPad + ".yaw"));
        return tele;
    }

    private void ensureDefaultConfig() {
        if (new File(this.getDataFolder(), "config.yml").exists()) return;

        getLogger().info("No config found. Generating one...");
        getConfig().options().setHeader(List.of(
                "For material names, see https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html"));
        getConfig().addDefault("pads.properties.type_material", "STONE");
        getConfig().addDefault("pads.properties.nodestmsg_enabled", true);
        getConfig().addDefault("pads.properties.teleport_time", 3);
        getConfig().addDefault("messages.delete", "Teleport pad %pad% removed.");
        getConfig().addDefault("messages.link", "Successfully linked %pad% to %pad2%");
        getConfig().addDefault("messages.wait", "Teleporting in %time%, stay on the pad!");
        getConfig().addDefault("messages.unlink", "Set destination of %pad% to nowhere.");
        getConfig().addDefault("messages.teleport.enable", true);
        getConfig().addDefault("messages.teleport.message", "Successfully teleported to %pad%");
        getConfig().addDefault("messages.teleport.denied", "You are not allowed to use this pad!");
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private void loadConfigValues() {
        String materialName = getConfig().getString("pads.properties.type_material", "STONE");
        Material mat = Material.matchMaterial(materialName);
        if (mat != null && mat.isBlock()) {
            telepadMaterial = mat;
        } else {
            getLogger().warning("Invalid or non-block material '" + materialName + "' in config, defaulting to STONE");
            telepadMaterial = Material.STONE;
        }

        teleportTime = Math.max(1, getConfig().getInt("pads.properties.teleport_time", 3));
        noDestMessageEnabled = getConfig().getBoolean("pads.properties.nodestmsg_enabled", true);
        teleportMessage = getConfig().getString("messages.teleport.message", "Successfully teleported to %pad%");
        teleportDeniedMessage = getConfig().getString("messages.teleport.denied", "You are not allowed to use this pad!");
        teleportMessageEnabled = getConfig().getBoolean("messages.teleport.enable", true);
        waitMessage = getConfig().getString("messages.wait", "Teleporting in %time%, stay on the pad!");
    }

    private void ensurePadFile() {
        if (new File(getDataFolder(), "pads.yml").exists()) return;

        getLogger().info("No pads found! Generating pads file...");
        File configFile = new File(this.getDataFolder(), "pads.yml");
        try {
            configFile.createNewFile();
        } catch (IOException ex) {
            getLogger().severe("Error creating pads config file: " + ex.getMessage());
        }
        padConfig.options().setHeader(List.of(
                "Teleportation pads are stored in this config file. Manual editing is not recommended."));
        padConfig.options().parseComments(true);
    }

    private void loadPads() {
        ConfigurationSection padsSection = padConfig.getConfigurationSection("pads");
        if (padsSection == null) {
            getLogger().info("No pads were loaded!");
            return;
        }

        PluginManager pm = getServer().getPluginManager();
        for (String padName : padsSection.getKeys(false)) {
            if (!padConfig.contains("pads." + padName + ".X")) continue;

            Location padLoc = new Location(null,
                    padConfig.getInt("pads." + padName + ".X"),
                    padConfig.getInt("pads." + padName + ".Y"),
                    padConfig.getInt("pads." + padName + ".Z"));
            String dest = padConfig.getString("pads." + padName + ".dest");
            padLinks.put(padName, dest != null ? dest : "nowhere");
            padLocations.put(padLoc, padName);

            Permission perm = new Permission("telepads.pads." + padName, PermissionDefault.OP);
            if (pm.getPermission(perm.getName()) == null) {
                pm.addPermission(perm);
            }
        }
    }

    public void reloadProperties() {
        ensureDefaultConfig();
        loadConfigValues();
        ensurePadFile();

        padLocations.clear();
        padLinks.clear();
        loadPads();
    }
}
