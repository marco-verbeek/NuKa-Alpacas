package me.nexty.alpacas;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.nexty.alpacas.utils.Conf;
import me.nexty.alpacas.utils.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class Alpaca {
    private static Main PLUGIN = null;

    private Entity entity;

    private Hologram hologram;
    private boolean hologramShown;

    private final String name;
    private final Gender gender;

    private double hunger;
    private long lastFeed = -1;
    private double feedAmount = 0;

    private double happiness;
    private final ArrayList<Double> prevHappiness = new ArrayList<>();

    private double readiness;
    private double quality;

    private static final char EMPTY_PROGRESS = '⬜';
    private static final char FULL_PROGRESS = '⬛';

    private static BukkitTask BEHAVIOR_TASK = null;

    public Alpaca(Entity entity, String name, Gender gender){
        this.entity = entity;

        this.name = name;
        this.gender = gender;

        this.hunger = Conf.startingHunger;
        this.happiness = Conf.startingHappiness;
        this.readiness = Conf.startingReadiness;
        this.quality = Conf.startingQuality;
    }

    public Alpaca(String name, Gender gender, Location loc, double hunger, double happiness, double readiness, double quality){
        this.name = name;
        this.gender = gender;
        this.hunger = hunger;
        this.happiness = happiness;
        this.readiness = readiness;
        this.quality = quality;

        assert loc.getWorld() != null;

        this.entity = loc.getWorld().spawnEntity(loc, EntityType.LLAMA);
        this.entity.setMetadata("NUKA_ALPACA", new FixedMetadataValue(PLUGIN, true));
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
     * Displays the Hologram above the Alpaca's head. After Config.holoDisplayTime seconds, deletes it.
     * @return false in case the Hologram is null
     */
    private boolean displayHologram(){
        if(this.hologram == null) return false;
        hologramShown = true;

        new BukkitRunnable(){
            int time = 0;

            @Override
            public void run() {
                if(time >= Conf.holoDisplayTime*20){
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
        double min = Conf.minHungerLoss;
        double max = Conf.maxHungerLoss;

        // This random Value is divided by two because this method is called twice per hour.
        double randomValue = (ThreadLocalRandom.current().nextDouble(min, max) /2) * -1;

        alpaca.addHunger(randomValue);
    }

    private static void happinessBehavior(Alpaca alpaca){
        // These values are divided by six because this method is called six times per hour.
        double aloneFactor = Conf.aloneFactor / 6;
        double familyValue = Conf.familyValue / 6;
        double hungerFactor = Conf.hungerFactor / 6;
        double musicFactor = Conf.musicFactor / 6;

        double happyValue = 0;

        Logger logger = new Logger(PLUGIN);
        logger.write("[Happiness]");
        logger.write(alpaca.getName());

        int nearbyAlpacas = getNearbyAlpacas(alpaca);
        if(nearbyAlpacas <= 0){
            happyValue += aloneFactor;
            logger.write(String.format("Alone -> %.2f |", aloneFactor));
        } else {
            happyValue += nearbyAlpacas * familyValue;
            logger.write(String.format("Family of %d -> %.2f |", nearbyAlpacas, nearbyAlpacas * familyValue));
        }

        if(isMusicPlaying(alpaca)) happyValue += musicFactor;
        logger.write(String.format("Music: %b -> %.2f | ", isMusicPlaying(alpaca), isMusicPlaying(alpaca) ? musicFactor : 0));

        if(alpaca.getHunger() <= 12){
            happyValue += ((alpaca.getHunger() / 10) - 1.2) * hungerFactor;
            logger.write(String.format("Hunger is %.2f -> %.2f |", alpaca.getHunger(), ((alpaca.getHunger() / 10) - 1.2) * hungerFactor));
        } else {
            happyValue += ((alpaca.getHunger() / 10) * hungerFactor) * 0.5;
            logger.write(String.format("Hunger is %.2f -> %.2f |", alpaca.getHunger(), ((alpaca.getHunger() / 10) * hungerFactor) * 0.5));
        }

        logger.write(String.format("Total: %f |", happyValue));
        logger.write(String.format("%.2f + %.2f = %.2f", alpaca.getHappiness(), happyValue, alpaca.getHappiness() + happyValue));
        logger.print(Level.INFO);

        alpaca.addHappiness(happyValue);
    }

    private static void qualityBehavior(Alpaca alpaca){
        if(!alpaca.isReady()){
            double readinessValue = Conf.readinessValue / 6;
            alpaca.addReadiness(readinessValue);
        } else {
            double happinessFactor = Conf.happinessFactor / 6;
            double prevHappyFactor = Conf.prevHappyFactor / 6;

            double qualityValue = 0;

            Logger logger = new Logger(PLUGIN);
            logger.write("[QualityCheck]");
            logger.write(alpaca.getName());

            // TODO: how higher you get, how difficult it is to gain quality? set values in config
            boolean canProduceHigherTierWool = canProduceHigherTierWool(alpaca);
            if(canProduceHigherTierWool)
                qualityValue += happinessFactor;

            logger.write(String.format("canProduceHigherTierWool: %b -> %.2f", canProduceHigherTierWool, canProduceHigherTierWool ? happinessFactor : 0));

            double avgFactor = (alpaca.getHappiness() >= alpaca.getPrevHappinessAvg()) ? 1 : 0;
            qualityValue += prevHappyFactor * avgFactor;
            logger.write(String.format("previousHappinesses -> %.2f * %.2f = %.2f | ", prevHappyFactor, avgFactor, prevHappyFactor * avgFactor));

            logger.write(String.format("Total: %.2f", qualityValue));
            logger.print(Level.INFO);

            alpaca.addQuality(qualityValue);
        }
    }

    private static boolean canProduceHigherTierWool(Alpaca alpaca) {
        double tier = alpaca.getQuality() / 12.5;

        if(tier < 1 && alpaca.getHappiness() >= Conf.woolTier1) return true;
        if(tier >= 1 && tier < 2 && alpaca.getHappiness() >= Conf.woolTier2) return true;
        if(tier >= 2 && tier < 3 && alpaca.getHappiness() >= Conf.woolTier3) return true;

        return false;
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
                    if(centerLoc.getWorld().getBlockAt(x, y, z).getType() == Material.JUKEBOX)
                        return checkCurrentlyPlaying((Jukebox) centerLoc.getWorld().getBlockAt(x, y, z).getState());

        return false;
    }

    private static boolean checkCurrentlyPlaying(Jukebox jb){
        if(!jb.isPlaying()) return false;
        if(!jb.hasMetadata("NUKA_PLAYING")) return false;

        long previousPlay = jb.getMetadata("NUKA_PLAYING").get(0).asLong();
        long timeBetween = System.currentTimeMillis() - previousPlay;

        return timeBetween <= Conf.jukeboxPlayTime*60*1000;
    }

    public static boolean isFood(Material material){
        return Conf.acceptedFoods.get(material) != null;
    }

    public void feed(ItemStack food){
        if(this.feedAmount >= Conf.maxFeedAmount && !PLUGIN.DEBUG){
            if((System.currentTimeMillis() - this.lastFeed) >= Conf.feedDelay*60*60*1000) {
                this.feedAmount = 0;
            } else return;
        }

        double feedValue = Conf.acceptedFoods.getOrDefault(food.getType(), 0.0);

        this.feedAmount += feedValue;
        this.lastFeed = System.currentTimeMillis();

        food.setAmount(food.getAmount() - 1);
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

    public String toString(){
        return String.format("%s|%s|%s|%f|%f|%f|%f", this.getName(), this.getGender().name(), loc2str(this.getLocation()), this.getHunger(), this.getHappiness(), this.getReadiness(), this.getQuality());
    }

    private static String loc2str(Location loc){
        return String.format("%s:%f:%f:%f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

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
    public String getName() { return this.name; }
    public double getHunger() { return this.hunger; }
    public double getHappiness() { return this.happiness; }
    public double getQuality() { return this.quality; }
    public double getReadiness() { return this.readiness; }
    public Gender getGender() { return gender; }

    public boolean isReady() { return this.readiness == 100; }
}
