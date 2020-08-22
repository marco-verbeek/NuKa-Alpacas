package me.nexty.alpacas;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EventListener implements Listener {
    private Main plugin;

    public EventListener(Main plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event == null) return;
        if(event.getRightClicked().getType() != EntityType.LLAMA) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if(!entity.hasMetadata("NUKA_ALPACA")) return;

        Alpaca alpaca = this.plugin.getAlpaca(entity.getUniqueId());
        if(alpaca == null) return;

        alpaca.refreshHologram();

        player.sendMessage("[Alpaca] Hunger: %f, ");

        // TODO: feed Alpaca
    }
}
