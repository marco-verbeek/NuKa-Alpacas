package me.nexty.alpacas;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
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
    private long lastFeed = -1;
    private double feedAmount = 0;

    private double happiness;
    private ArrayList<Double> prevHappiness = new ArrayList<>();

    private double quality;
    private double readiness;

    private static final char EMPTY_PROGRESS = '⬜';
    private static final char FULL_PROGRESS = '⬛';

    private static BukkitTask BEHAVIOR_TASK = null;

    private static HashMap<Material, Double> ACCEPTED_FOOD;

    public Alpaca(Entity entity, String name, Gender gender){
        this.entity = entity;

        this.name = name;
        this.gender = gender;

        this.hunger = PLUGIN.getConfig().getDouble("alpaca-behavior.starting-hunger", 6*12);
        this.happiness = PLUGIN.getConfig().getDouble("alpaca-behavior.starting-happiness", 4*12);
        this.readiness = PLUGIN.getConfig().getDouble("alpaca-behavior.starting-readiness", 12);
        this.quality = PLUGIN.getConfig().getDouble("alpaca-behavior.starting-quality", 0);
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
        this.hologram.insertTextLine(1, ChatColor.translateAlternateColorCodes('&', String.format("&6%s &f(&o%.2f)", formatProgress(this.happiness), this.happiness)));

        String readyOrQuality;
        if(readiness == 100)
            readyOrQuality = String.format("&b%s (&o%.2f)", formatProgress(this.quality), this.quality);
        else
            readyOrQuality = String.format("&f%s (&o%.2f)", formatProgress(this.readiness), this.readiness);

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
        if(BEHAVIOR_TASK != null) return;

        BEHAVIOR_TASK = new BukkitRunnable(){
            int cycle = 0;

            @Override
            public void run() {
                // Every half hour, make alpacas lose between 0.3 and 0.5 hunger
                if(cycle % 6 == 0){
                    PLUGIN.getLogger().info("[Alpacas] Half an hour has passed.");

                    PLUGIN.getAlpacas().forEach(Alpaca::hungerBehavior);
                }

                // Every 10 minutes, update HAPPINESS and READINESS/QUALITY depending on hunger, hasMusic, isAlone
                if(cycle % 10 == 0){
                    PLUGIN.getLogger().info("[Alpacas] 10 minutes have passed.");

                    PLUGIN.getAlpacas().forEach(Alpaca::happinessBehavior);
                    PLUGIN.getAlpacas().forEach(Alpaca::qualityBehavior);
                }

                // Every 12 hours, save happiness into prevHappinesses
                if(cycle % 720 == 0){
                    PLUGIN.getAlpacas().forEach(Alpaca::prevHappinessBehavior);
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
                    PLUGIN.getLogger().info("[Alpacas] 10 seconds have passed.");

                    PLUGIN.getAlpacas().forEach(Alpaca::happinessBehavior);
                    PLUGIN.getAlpacas().forEach(Alpaca::qualityBehavior);
                }

                if(cycle % 60 == 0){
                    PLUGIN.getLogger().info("[Alpacas] A whole minute has passed. Saving previous happinesses");
                    PLUGIN.getAlpacas().forEach(Alpaca::prevHappinessBehavior);
                }

                cycle++;
            }
        }.runTaskTimer(PLUGIN, 0, 20);
    }

    private static void hungerBehavior(Alpaca alpaca){
        double min = PLUGIN.getConfig().getDouble("alpaca-behavior.min-hunger-value", 0.6);
        double max = PLUGIN.getConfig().getDouble("alpaca-behavior.max-hunger-value", 1.0);

        // This random Value is divided by two because this method is called twice per hour.
        double randomValue = (ThreadLocalRandom.current().nextDouble(min, max) /2) * -1;

        alpaca.addHunger(randomValue);
    }

    // TODO: abstract these values in config.
    private static void happinessBehavior(Alpaca alpaca){
        double aloneFactor = PLUGIN.getConfig().getDouble("alpaca-behavior.alone-factor", -1.0);
        double familyValue = PLUGIN.getConfig().getDouble("alpaca-behavior.family-factor", 0.1);
        double hungerFactor = PLUGIN.getConfig().getDouble("alpaca-behavior.hunger-factor", 0.25);
        double musicFactor = PLUGIN.getConfig().getDouble("alpaca-behavior.music-factor", 0.1);

        double happyValue = 0;

        String debug = "[Happiness] ";

        int nearbyAlpacas = getNearbyAlpacas(alpaca);
        if(nearbyAlpacas <= 0){
            happyValue += aloneFactor;
            debug += String.format("Family of %d -> %.2f | ", nearbyAlpacas, aloneFactor);
        } else {
            happyValue += nearbyAlpacas * familyValue;
            debug += String.format("Family of %d -> %.2f | ", nearbyAlpacas, nearbyAlpacas * familyValue);
        }

        if(isMusicPlaying(alpaca)) happyValue += musicFactor;
        debug += String.format("Music: %b | ", isMusicPlaying(alpaca));

        if(alpaca.getHunger() <= 12){
            happyValue -= ((alpaca.getHunger() / 10) - 1.2) * hungerFactor;
            debug += String.format("Hunger: %.2f | ", ((alpaca.getHunger() / 10) - 1.2) * hungerFactor);
        } else {
            happyValue += ((alpaca.getHunger() / 10) * hungerFactor) * 0.5;
            debug += String.format("Hunger was %.2f -> %.2f | ", alpaca.getHunger(), ((alpaca.getHunger() / 10) * hungerFactor) * 0.5);
        }

        debug += String.format("Total: %f | ", happyValue);
        debug += String.format("%.2f + %.2f = %.2f", alpaca.getHappiness(), happyValue, alpaca.getHappiness() + happyValue);
        PLUGIN.getLogger().info(debug);

        alpaca.addHappiness(happyValue);
    }

    private static void qualityBehavior(Alpaca alpaca){
        if(!alpaca.isReady()){
            double readinessValue = PLUGIN.getConfig().getDouble("alpaca-behavior.readiness-factor", 25);
            alpaca.addReadiness(readinessValue);
        } else {
            double happinessFactor = PLUGIN.getConfig().getDouble("alpaca-behavior.happiness-factor", 0.1);
            double prevHappyFactor = PLUGIN.getConfig().getDouble("alpaca-behavior.prev-happiness-factor", 0.05);

            double qualityValue = 0;

            String debug = "[QualityCheck] ";

            qualityValue += happinessFactor * ((alpaca.getHappiness() - 50) / 100);
            debug += String.format("currentHappiness -> %.2f * ((%.2f - 50) / 100) = %.2f | ", happinessFactor, alpaca.getHappiness(), happinessFactor * ((alpaca.getHappiness() - 50) / 100));

            double avgFactor = (alpaca.getHappiness() >= alpaca.getPrevHappinessAvg()) ? 1 : 0;
            qualityValue += prevHappyFactor * avgFactor;
            debug += String.format("previousHappinesses -> %.2f * %.2f = %.2f | ", prevHappyFactor, avgFactor, prevHappyFactor * avgFactor);

            debug += String.format("Total: %.2f", qualityValue);
            PLUGIN.getLogger().info(debug);

            alpaca.addQuality(qualityValue);
        }
    }

    private static void prevHappinessBehavior(Alpaca alpaca){
        alpaca.addPrevHappiness(alpaca.getHappiness());
    }

    private static int getNearbyAlpacas(Alpaca alpaca) {
        return (int) alpaca.getEntity().getNearbyEntities(15, 15, 15).stream().filter(nearbyEntity -> nearbyEntity.getType() == EntityType.LLAMA).count();
    }

    private static boolean isMusicPlaying(Alpaca alpaca) {
        Location centerLoc = alpaca.getLocation();
        int radius = 15;

        for(int x = centerLoc.getBlockX() - radius; x <= centerLoc.getBlockX() + radius; x++)
            for(int y = centerLoc.getBlockY() - radius; y <= centerLoc.getBlockY() + radius; y++)
                for(int z = centerLoc.getBlockZ() - radius; z <= centerLoc.getBlockZ() + radius; z++)
                    if(centerLoc.getWorld().getBlockAt(x, y, z).getType() == Material.JUKEBOX && ((Jukebox) centerLoc.getWorld().getBlockAt(x, y, z).getState()).isPlaying())
                        return true;

        return false;
    }

    public static void loadFoodValues() {
        if(ACCEPTED_FOOD != null) return;

        ACCEPTED_FOOD = new HashMap<Material, Double>() {{
            for(String food : PLUGIN.getConfig().getStringList("alpaca-behavior.food")){
                String[] data = food.split(" ");

                try {
                    put(Material.valueOf(data[0]), Double.parseDouble(data[1]));

                    if(PLUGIN.DEBUG) PLUGIN.getLogger().info(String.format("[Alpacas] Added food item '%s'", food));
                } catch (IllegalArgumentException ex){
                    PLUGIN.getLogger().severe(String.format("[Alpacas] Could not add food item '%s'", food));
                }
            }
        }};
    }

    public static boolean isFood(Material material){
        return ACCEPTED_FOOD.get(material) != null;
    }

    // TODO: abstract feed values to config
    public void feed(Material food){
        // TODO remove this debug false
        if(this.feedAmount >= PLUGIN.getConfig().getDouble("alpaca-behavior.feed-amount", 12) && false){
            if((System.currentTimeMillis() - this.lastFeed) >= PLUGIN.getConfig().getDouble("alpaca-behavior.feed-delay", 8)*60*60*1000) {
                this.feedAmount = 0;
            } else return;
        }

        double feedValue = ACCEPTED_FOOD.getOrDefault(food, 0.0);

        this.feedAmount += feedValue;
        this.lastFeed = System.currentTimeMillis();

        this.addHunger(feedValue);
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

        return str.toString();
    }

    public void setEntity(Entity entity) { this.entity = entity; }
    public void setHunger(double hunger) { this.hunger = hunger; }
    public void setHappiness(double happiness) { this.happiness = happiness; }

    public void setReadiness(double readiness) { this.readiness = readiness; }
    public void setQuality(double quality) { this.quality = quality; }

    public void addHunger(double value) {
        this.hunger = Math.max(0, Math.min(100, this.hunger + value));
    }
    public void addHappiness(double value) {
        this.happiness = Math.max(0, Math.min(100, this.happiness + value));
    }
    private void addReadiness(double value) {
        this.readiness = Math.max(0, Math.min(100, this.readiness + value));
    }
    private void addQuality(double value) {
        this.quality = Math.max(0, Math.min(100, this.quality + value));
    }

    public void addPrevHappiness(double prevHappiness) {
        this.prevHappiness.add(prevHappiness);

        if(this.prevHappiness.size() >= 10)
            this.prevHappiness.remove(0);
    }

    public double getPrevHappinessAvg() {
        double avg = 0;

        for(double d : this.prevHappiness)
            avg += d;

        avg /= this.prevHappiness.size();

        return avg;
    }


    public Entity getEntity() { return this.entity; }
    public Location getLocation() { return this.entity.getLocation(); }
    public double getHunger() { return this.hunger; }
    public double getHappiness() { return this.happiness; }
    public double getQuality() { return this.quality; }
    public double getReadiness() { return this.readiness; }

    public boolean isReady() { return this.readiness == 100; }
}
