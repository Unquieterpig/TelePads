package me.wizzledonker.plugins.telepads.config;

import java.io.File;
import java.io.IOException;
import me.wizzledonker.plugins.telepads.Telepads;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class padConfiguration {
    
    FileConfiguration padConfig = null;
    File padConfigFile = null;
    
    private static Telepads plugin;
    
    public padConfiguration(Telepads instance) {
        plugin = instance;
    }
    
    public void reloadPadConfig() {
        if (padConfigFile == null) {
            padConfigFile = new File(plugin.getDataFolder(), "pads.yml");
        }
        
        padConfig = YamlConfiguration.loadConfiguration(padConfigFile);
    }
    
    public FileConfiguration getPadConfig() {
        if (padConfig == null) {
            reloadPadConfig();
        }
        return padConfig;
    }
    
    public void savePadConfig() {
        if (padConfig == null || padConfigFile == null) {
            return;
        }
        try {
            padConfig.save(padConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("A problem occurred saving the pad config file: " + ex);
        }
    }
    
}
