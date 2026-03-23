package me.wizzledonker.plugins.telepads;

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TelepadCommands implements CommandExecutor {

    private final Telepads plugin;

    public TelepadCommands(Telepads plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        return switch (cmd.getName().toLowerCase()) {
            case "createpad" -> handleCreatePad(sender, args);
            case "padlink" -> handlePadLink(sender, args);
            case "padunlink" -> handlePadUnlink(sender, args);
            case "delpad" -> handleDelPad(sender, args);
            case "linkpadhere" -> handleLinkPadHere(sender, args);
            case "padlist" -> handlePadList(sender);
            default -> false;
        };
    }

    private boolean handleCreatePad(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command!");
            return true;
        }
        if (args.length != 1) {
            return false;
        }
        plugin.createPad(player, args[0]);
        return true;
    }

    private boolean handlePadLink(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command!");
            return true;
        }
        if (args.length != 2) {
            return false;
        }
        plugin.linkPads(args[0], args[1], player);
        return true;
    }

    private boolean handlePadUnlink(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return false;
        }
        plugin.unlinkPad(args[0], sender);
        return true;
    }

    private boolean handleDelPad(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return false;
        }
        plugin.deletePad(sender, args[0]);
        return true;
    }

    private boolean handleLinkPadHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players may use this command!");
            return true;
        }
        if (args.length != 1) {
            return false;
        }
        plugin.linkPadHere(player, args[0]);
        return true;
    }

    private boolean handlePadList(CommandSender sender) {
        if (!sender.hasPermission("telepads.list")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "------" + ChatColor.RED + "Telepads" + ChatColor.GREEN + "------");

        Map<String, String> links = plugin.getTeleLinks();
        if (links.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "There are currently no pads.");
            return true;
        }
        for (Map.Entry<String, String> entry : links.entrySet()) {
            sender.sendMessage(" - " + entry.getKey());
            sender.sendMessage(ChatColor.AQUA + "  * linked to " + entry.getValue());
        }
        return true;
    }
}
