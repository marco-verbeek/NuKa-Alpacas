package me.nexty.alpacas;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class Main extends JavaPlugin {
    private HashMap<Integer, Alpaca> alpacas = new HashMap<>();
    private int currentAmountAlpacas = 0;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        Alpaca.setPlugin(this);

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");

            this.setEnabled(false);
            return;
        }

        // TODO: get all Alpacas, add them to HashMap, forEach = currentAmount++
    }

    @Override
    public void onDisable() {
        // TODO: forEach Alpaca in HashMap, save to YML.
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player && label.equalsIgnoreCase("nuka")){
            Entity alpaca;

            try {
                alpaca = Bukkit.getWorld("world").spawnEntity(((Player) sender).getLocation(), EntityType.LLAMA);
            } catch(NullPointerException ex){
                ex.printStackTrace();
                return false;
            }

            alpaca.setMetadata("NUKA_ALPACA", new FixedMetadataValue(this, true));
        }

        return true;
    }
}
