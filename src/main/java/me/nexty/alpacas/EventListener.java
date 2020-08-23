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

        player.sendMessage(String.format("Hunger: %f | Happy: %f | Ready: %f | Quality: %f", alpaca.getHunger(), alpaca.getHappiness(), alpaca.getReadiness(), alpaca.getQuality()));

        double hungerValue = Alpaca.getFoodValue(player.getInventory().getItemInMainHand().getType());
        if(hungerValue > 0) alpaca.addHunger(hungerValue);

        alpaca.refreshHologram();
    }
}
