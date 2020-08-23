package me.nexty.alpacas;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Alpaca {
    // TODO: serialize in order to save it in yml
    // TODO: softdepend SaberFaction, if enabled: save Faction Name

    private static Main PLUGIN = null;

    private Entity entity;

    private Hologram hologram;
    private boolean hologramShown;

    private String name;
    private Gender gender;

    private double hunger;
    private double happiness;
    private double quality;
    private double readiness;

    private static final char EMPTY_PROGRESS = '⬜';
    private static final char FULL_PROGRESS = '⬛';

    private static BukkitTask BEHAVIOR_TASK = null;

    private static final HashMap<Material, Double> ACCEPTED_FOOD = new HashMap<Material, Double>() {{
        put(Material.APPLE, 10.0);
        put(Material.CARROT, 8.0);
        put(Material.GRASS, 2.0);
    }};

    public Alpaca(Entity entity, String name, Gender gender){
        this.entity = entity;

        this.name = name;
        this.gender = gender;

        this.hunger = 6*12;
        this.happiness = 4*12;
        this.quality = 0;
        this.readiness = 12;
    }

    /**
     * Creates the Hologram linked to this Alpaca if none already exist.
     * Returns false if there was no need to create a new one.
     * @return whether the creation was needed and successful
     */
    public boolean createHologram(){
        if(this.hologram != null) return false;

        this.hologram = HologramsAPI.createHologram(PLUGIN, getLocation().add(0, 3, 0));
        return true;
    }

    /**
     * Refreshes the data displayed on the Hologram linked to this Alpaca and displays it for 5 seconds.
     * Creates the Hologram using @createHologram in case the hologram is null.
     * @return whether the refresh and display was successful
     */
    public boolean refreshHologram(){
        if(this.hologram == null) createHologram();

        this.hologram.clearLines();
        this.hologram.insertTextLine(0, ChatColor.translateAlternateColorCodes('&', String.format("&b%s &f(&7%c&f)", this.name, this.gender.getAbrv())));
        this.hologram.insertTextLine(1, ChatColor.translateAlternateColorCodes('&', String.format("&6%s", formatProgress(this.happiness))));

        String readyOrQuality;
        if(readiness == 100)
            readyOrQuality = String.format("&b%s", formatProgress(this.quality));
        else
            readyOrQuality = String.format("&f%s", formatProgress(this.readiness));

        this.hologram.insertTextLine(2, ChatColor.translateAlternateColorCodes('&', readyOrQuality));

        if(hologramShown) return true;
        return displayHologram();
    }

    /**
     * Displays the Hologram above the Alpaca's head. After 5 seconds, deletes it.
     * @return false in case the Hologram is null
     */
    private boolean displayHologram(){
        if(this.hologram == null) return false;
        hologramShown = true;

        new BukkitRunnable(){
            int time = 0;

            @Override
            public void run() {
                if(time >= 5*20){
                    hologram.delete();
                    hologram = null;
                    hologramShown = false;

                    cancel();
                    return;
                }

                Location loc = getLocation().add(0, 3, 0);
                hologram.teleport(loc);

                time++;
            }
        }.runTaskTimer(PLUGIN, 0, 1);

        return true;
    }

    /**
     * The Behavior Task gets executed once every 5 minutes
     * It updates every Alpaca's happiness, readiness and wool quality every 10 minutes.
     * Every hour, Alpacas lose between 0.6 and 1 hunger.
     */
    public static void startBehavior(){
        if(BEHAVIOR_TASK != null){
            // TODO: cancel current task? re-setup behavior? do nothing?
            return;
        }

        BEHAVIOR_TASK = new BukkitRunnable(){
            int cycle = 0;

            @Override
            public void run() {
                // Every half hour, make alpacas lose between 0.3 and 0.5 hunger
                if(cycle % 6 == 0){
                    PLUGIN.getLogger().info("[Alpacas] Half an hour has passed. Updating hunger.");

                    PLUGIN.getAlpacas().forEach(Alpaca::hungerBehavior);
                }

                // Every 10 minutes, update HAPPINESS and READINESS/QUALITY depending on hunger, hasMusic, isAlone
                if(cycle % 10 == 0){
                    PLUGIN.getLogger().info("[Alpacas] 10 minutes have passed. Updating happiness and quality.");

                    PLUGIN.getAlpacas().forEach(Alpaca::happinessBehavior);
                    PLUGIN.getAlpacas().forEach(Alpaca::qualityBehavior);
                }

                cycle += 5;
            }
        }.runTaskTimer(PLUGIN, 0, 5*60*20);
    }

    public static void startTestingBehavior(){
        if(BEHAVIOR_TASK != null) return;

        BEHAVIOR_TASK = new BukkitRunnable(){
            int cycle = 0;

            @Override
            public void run() {
                if(cycle % 2 == 0){
                    PLUGIN.getAlpacas().forEach(Alpaca::hungerBehavior);
                }

                if(cycle % 10 == 0){
                    PLUGIN.getAlpacas().forEach(Alpaca::happinessBehavior);
                    PLUGIN.getAlpacas().forEach(Alpaca::qualityBehavior);
                }

                cycle++;
            }
        }.runTaskTimer(PLUGIN, 0, 20);
    }

    private static void hungerBehavior(Alpaca alpaca){
        double randomValue = ThreadLocalRandom.current().nextDouble(0.30, 0.51) * -1;
        alpaca.addHunger(randomValue);
    }

    private static void happinessBehavior(Alpaca alpaca){
        double aloneFactor = -1.0;
        double familyValue = 0.1;
        double hungerFactor = 0.25;
        double musicFactor = 0.20;

        double happyValue = 0;

        String debug = "";

        int nearbyAlpacas = getNearbyAlpacas(alpaca);
        if(nearbyAlpacas <= 0){
            happyValue += aloneFactor;
            debug += String.format("Family of %d -> %f | ", nearbyAlpacas, aloneFactor);
        } else {
            happyValue += nearbyAlpacas * familyValue;
            debug += String.format("Family of %d -> %f | ", nearbyAlpacas, nearbyAlpacas * familyValue);
        }

        if(isMusicPlaying(alpaca)) happyValue += musicFactor;
        debug += String.format("Music: %b | ", isMusicPlaying(alpaca));

        if(alpaca.getHunger() <= 12){
            happyValue -= (1.2 - (alpaca.getHunger() / 10)) * hungerFactor;
            debug += String.format("Hunger: %f | ", (1.2 - (alpaca.getHunger() / 10)) * hungerFactor);
        } else {
            happyValue += (alpaca.getHunger() / 10) * hungerFactor;
            debug += String.format("Hunger was %f -> %f | ", alpaca.getHunger(), (alpaca.getHunger() / 10) * hungerFactor);
        }

        debug += String.format("Total: %f", happyValue);
        PLUGIN.getLogger().info(debug);

        alpaca.addHappiness(happyValue);
    }

    private static void qualityBehavior(Alpaca alpaca){
        if(!alpaca.isReady()){
            double readinessValue = 25;
            alpaca.addReadiness(readinessValue);
        } else {
            // TODO: value depends on happiness and previous happinesses
            alpaca.addQuality(12);
        }
    }

    private static int getNearbyAlpacas(Alpaca alpaca) {
        return (int) alpaca.getEntity().getNearbyEntities(15, 15, 15).stream().filter(nearbyEntity -> nearbyEntity.getType() == EntityType.LLAMA).count();
    }

    private static boolean isMusicPlaying(Alpaca alpaca) {
        // TODO
        return true;
    }

    public static double getFoodValue(Material material){
        return ACCEPTED_FOOD.getOrDefault(material, 0.0);
    }

    public static void setPlugin(Main plugin){
        if(plugin == null) return;

        PLUGIN = plugin;
    }

    private static String formatProgress(double percent){
        StringBuilder str = new StringBuilder();

        double cubeAmount = percent/12.5;
        for(int i=1; i<=8; i++){
            str.append((i <= cubeAmount) ? FULL_PROGRESS : EMPTY_PROGRESS);
        }

        PLUGIN.getLogger().severe("[Alpacas] Percent was " + percent + ", therefore I created progressbar: " + str);

        return str.toString();
    }

    public void setEntity(Entity entity) { this.entity = entity; }
    public void setHunger(double hunger) { this.hunger = hunger; }
    public void setHappiness(double happiness) { this.happiness = happiness; }
    public void setReadiness(double readiness) { this.readiness = readiness; }
    public void setQuality(double quality) { this.quality = quality; }

    public void addHunger(double value) {
        this.hunger += value;
        if(this.hunger > 100) this.hunger = 100;
    }
    public void addHappiness(double value) {
        this.happiness += value;
        if(this.happiness > 100) this.happiness = 100;
    }
    private void addReadiness(double value) {
        this.readiness += value;
        if(this.readiness > 100) this.readiness = 100;
    }
    private void addQuality(double value) {
        this.quality += value;
        if(this.quality > 100) this.quality = 100;
    }

    public Entity getEntity() { return this.entity; }
    public Location getLocation() { return this.entity.getLocation(); }
    public double getHunger() { return this.hunger; }
    public double getHappiness() { return this.happiness; }
    public double getQuality() { return this.quality; }
    public double getReadiness() { return this.readiness; }

    public boolean isReady() { return this.readiness == 100; }
}
