package me.nexty.alpacas;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin {
    private final HashMap<UUID, Alpaca> alpacas = new HashMap<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);

        Alpaca.setPlugin(this);
        //Alpaca.startBehavior();
        Alpaca.startTestingBehavior();

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");

            this.setEnabled(false);
            return;
        }

        // TODO: get all Alpacas, add them to HashMap
    }

    @Override
    public void onDisable() {
        // TODO: forEach Alpaca in HashMap, save to YML
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player && label.equalsIgnoreCase("nuka")){
            Entity entity;

            try {
                entity = Bukkit.getWorld("world").spawnEntity(((Player) sender).getLocation(), EntityType.LLAMA);
            } catch(NullPointerException ex){
                ex.printStackTrace();
                return false;
            }

            entity.setMetadata("NUKA_ALPACA", new FixedMetadataValue(this, true));

            Alpaca alpaca = new Alpaca(entity, "Alpi", Gender.MALE);
            this.alpacas.put(entity.getUniqueId(), alpaca);
        }

        return true;
    }

    public Alpaca getAlpaca(UUID uniqueId) {
        return this.alpacas.get(uniqueId);
    }

    public Collection<Alpaca> getAlpacas(){
        return this.alpacas.values();
    }
}
