package me.nexty.alpacas;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class Alpaca {
    // TODO: serialize in order to save it in yml
    // TODO: softdepend SaberFaction, if enabled: save Faction Name

    private static Main PLUGIN = null;

    private UUID id;
    private Entity entity;
    private Location location;

    private Hologram hologram;
    private boolean hologramShown;

    private String name;
    private Gender gender;

    private double hunger;
    private double happiness;
    private double quality;
    private double ready;

    private static char EMPTY_PROGRESS = '⬜';
    private static char FULL_PROGRESS = '⬛';

    public Alpaca(String name, Gender gender){
        this.id = UUID.randomUUID();
        this.name = name;
        this.gender = gender;

        this.hunger = 6*12;
        this.happiness = 4*12;
        this.quality = 0;
        this.ready = 1*12;
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
        this.hologram.insertTextLine(0, ChatColor.translateAlternateColorCodes('&', String.format("&b%s &f(&7%s&f)", this.name, this.gender.name)));
        this.hologram.insertTextLine(1, ChatColor.translateAlternateColorCodes('&', String.format("&6%s", formatProgress(this.happiness))));

        String readyOrQuality;
        if(ready == 100)
            readyOrQuality = String.format("&b%s", formatProgress(this.quality));
        else
            readyOrQuality = String.format("&f%s", formatProgress(this.ready));

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

    private static String formatProgress(double percent){
        StringBuilder str = new StringBuilder();

        double cubeAmount = percent/12.5;
        for(int i=1; i<=8; i++){
            str.append((i <= cubeAmount) ? FULL_PROGRESS : EMPTY_PROGRESS);
        }

        PLUGIN.getLogger().severe("[Alpacas] Percent was " + percent + ", therefore I created progressbar: " + str);

        return str.toString();
    }

    public static void setPlugin(Main plugin){
        if(plugin == null) return;

        PLUGIN = plugin;
    }

    public void setLocation(Location location){ this.location = location; }
    public void setHunger(double hunger) { this.hunger = hunger; }
    public void setHappiness(double happiness) { this.happiness = happiness; }
    public void setReady(double ready) { this.ready = ready; }
    public void setEntity(Entity entity) { this.entity = entity; }

    public UUID getId() { return id; }
    public Location getLocation() { return this.entity.getLocation(); }
    public Entity getEntity() { return entity; }
}
