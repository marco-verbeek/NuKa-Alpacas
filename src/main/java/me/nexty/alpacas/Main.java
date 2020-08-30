package me.nexty.alpacas;

import me.nexty.alpacas.utils.Conf;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin {
    private final HashMap<UUID, Alpaca> alpacas = new HashMap<>();
    private FileConfiguration db;
    private File dbFile;

    public final boolean DEBUG = this.getConfig().getBoolean("debug", false);

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.saveDefaultConfig();
        initDB();

        Conf.loadConfigValues(this.getConfig());

        Alpaca.setPlugin(this);

        if(this.DEBUG)
            Alpaca.startTestingBehavior();
        else
            Alpaca.startBehavior();

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");

            this.setEnabled(false);
            return;
        }

        loadDB();
    }

    @Override
    public void onDisable() {
        saveToDB();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player && label.equalsIgnoreCase("nuka")){
            Entity entity = Objects.requireNonNull(Bukkit.getWorld("world")).spawnEntity(((Player) sender).getLocation(), EntityType.LLAMA);
            entity.setMetadata("NUKA_ALPACA", new FixedMetadataValue(this, true));

            Alpaca alpaca = new Alpaca(entity, (args.length == 0) ? "Alpi" : args[0], Gender.MALE);
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

    private void initDB() {
        final String fileName = "db.yml";

        this.dbFile = new File(getDataFolder(), fileName);
        if (!this.dbFile.exists()) {
            this.dbFile.getParentFile().mkdirs();
            saveResource(fileName, false);
        }

        this.db = new YamlConfiguration();
        try {
            this.db.load(this.dbFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void loadDB() {
        for(String strAlpaca : this.db.getStringList("alpacas")){
            String[] values = strAlpaca.split("\\|");

            Alpaca alpaca = new Alpaca(values[0], Gender.valueOf(values[1]), str2loc(values[2]), Double.parseDouble(values[3]), Double.parseDouble(values[4]), Double.parseDouble(values[5]), Double.parseDouble(values[6]));
            this.alpacas.put(alpaca.getEntity().getUniqueId(), alpaca);
        }
    }

    private void saveToDB(){
        ArrayList<String> list = new ArrayList<>();

        this.alpacas.values().forEach(alpaca -> list.add(alpaca.toString()));
        this.alpacas.values().forEach(alpaca -> alpaca.getEntity().remove());

        this.db.set("alpacas", list);

        try { this.db.save(this.dbFile); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private static Location str2loc(String str){
        String[] values = str.split("\\:");
        return new Location(Bukkit.getWorld(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]), Double.parseDouble(values[3]));
    }
}
