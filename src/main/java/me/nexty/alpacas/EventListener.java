package me.nexty.alpacas;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class EventListener implements Listener {
    private Main plugin;
    private Alpaca alpaca;

    public EventListener(Main plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event == null || event.getRightClicked() == null) return;
        if(event.getRightClicked().getType() != EntityType.LLAMA) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if(!entity.hasMetadata("NUKA_ALPACA")) return;

        player.sendMessage("You have interacted with an Alpaca!");

        if(alpaca != null) {
            alpaca.refreshHologram();
            return;
        }

        this.alpaca = new Alpaca("Alpi", Gender.MALE);
        alpaca.setEntity(entity);
        alpaca.refreshHologram();

        // test progress bar, TODO: remove
        new BukkitRunnable(){
            double testVal = 0;

            @Override
            public void run() {
                if(testVal >= 100) testVal = 0;
                testVal += 12.5;

                alpaca.setHappiness(testVal);
                alpaca.setReady(testVal);
            }
        }.runTaskTimer(this.plugin, 0, 30);
    }
}
