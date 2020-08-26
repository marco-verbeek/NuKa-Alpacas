package me.nexty.alpacas;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityMountEvent;

public class EventListener implements Listener {
    private final Main plugin;

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

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if(Alpaca.isFood(heldItem.getType()))
            alpaca.feed(heldItem);

        if(this.plugin.DEBUG)
            player.sendMessage(String.format("Hunger: %.00f | Happy: %.00f | Ready: %.00f | Quality: %.00f", alpaca.getHunger(), alpaca.getHappiness(), alpaca.getReadiness(), alpaca.getQuality()));

        alpaca.refreshHologram();
    }

    @EventHandler
    public void onEntityMount(EntityMountEvent event){
        if(event == null) return;

        if(!event.getMount().hasMetadata("NUKA_ALPACA")) return;

        // Cannot mount an Alpaca
        event.setCancelled(true);
    }
}
