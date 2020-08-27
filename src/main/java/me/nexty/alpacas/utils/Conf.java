package me.nexty.alpacas.utils;

import org.bukkit.configuration.file.FileConfiguration;

public class Conf {
    public static double jukeboxPlayTime = 5;
    public static double holoDisplayTime = 10;

    public static double startingHunger = 72;
    public static double startingHappiness = 48;
    public static double startingReadiness = 12;
    public static double startingQuality = 0;

    public static double aloneFactor = -6;
    public static double familyValue = 0.6;
    public static double hungerFactor = 1.5;
    public static double musicFactor = 0.6;

    public static double happinessFactor = 0.6;
    public static double prevHappyFactor = 0.3;

    public static double readinessValue = 150;

    public static double minHungerLoss = 0.6;
    public static double maxHungerLoss = 1.0;

    public static double feedDelay = 8;
    public static double maxFeedAmount = 12;

    public static double woolTier1 = 25;
    public static double woolTier2 = 50;
    public static double woolTier3 = 75;

    public static void loadConfigValues(FileConfiguration config){
        jukeboxPlayTime = config.getDouble("holo-display-time", 10);
        holoDisplayTime = config.getDouble("jukebox-play-time", 5);

        startingHunger = config.getDouble("alpaca-behavior.starting-hunger", 6*12);
        startingHappiness = config.getDouble("alpaca-behavior.starting-happiness", 6*12);
        startingReadiness = config.getDouble("alpaca-behavior.starting-readiness", 6*12);
        startingQuality = config.getDouble("alpaca-behavior.starting-quality", 6*12);

        aloneFactor = config.getDouble("alpaca-behavior.alone-factor", -6.0);
        familyValue = config.getDouble("alpaca-behavior.family-factor", 0.6);
        hungerFactor = config.getDouble("alpaca-behavior.hunger-factor", 1.5);
        musicFactor = config.getDouble("alpaca-behavior.music-factor", 0.6);

        happinessFactor = config.getDouble("alpaca-behavior.happiness-factor", 0.6);
        prevHappyFactor = config.getDouble("alpaca-behavior.prev-happiness-factor", 0.3);

        readinessValue = config.getDouble("alpaca-behavior.readiness-factor", 150);

        minHungerLoss = config.getDouble("alpaca-behavior.min-hunger-value", 0.6);
        maxHungerLoss = config.getDouble("alpaca-behavior.max-hunger-value", 1.0);

        feedDelay = config.getDouble("alpaca-behavior.feed-delay", 8);
        maxFeedAmount = config.getDouble("alpaca-behavior.feed-amount", 12);

        woolTier1 = config.getDouble("alpaca-behavior.starting-hunger", 6*12);
        woolTier2 = config.getDouble("alpaca-behavior.starting-hunger", 6*12);
        woolTier3 = config.getDouble("alpaca-behavior.starting-hunger", 6*12);
    }
}
