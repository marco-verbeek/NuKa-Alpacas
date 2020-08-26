package me.nexty.alpacas;

import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.ArrayList;

public class EventListener implements Listener {
    private final Main plugin;
    private static final ArrayList<Material> DISKS = new ArrayList<Material>() {{
        add(Material.MUSIC_DISC_11);
        add(Material.MUSIC_DISC_13);
        add(Material.MUSIC_DISC_BLOCKS);
        add(Material.MUSIC_DISC_CAT);
        add(Material.MUSIC_DISC_CHIRP);
        add(Material.MUSIC_DISC_FAR);
        add(Material.MUSIC_DISC_MALL);
        add(Material.MUSIC_DISC_MELLOHI);
        add(Material.MUSIC_DISC_PIGSTEP);
        add(Material.MUSIC_DISC_STAL);
        add(Material.MUSIC_DISC_STRAD);
        add(Material.MUSIC_DISC_WAIT);
        add(Material.MUSIC_DISC_WARD);
    }};

    public EventListener(Main plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event){
        if(!event.getMount().hasMetadata("NUKA_ALPACA")) return;

        // Cannot mount an Alpaca
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event){
        if(event.getClickedBlock() instanceof Jukebox) return;

        Material held = event.getPlayer().getInventory().getItemInMainHand().getType();
        if(!DISKS.contains(held)) return;

        try {
            Jukebox jukebox = (Jukebox) event.getClickedBlock().getState();
            jukebox.setMetadata("NUKA_PLAYING", new FixedMetadataValue(this.plugin, System.currentTimeMillis()));
        } catch (NullPointerException ex){
            this.plugin.getLogger().severe("[Alpacas] NullPointerException during metadata setting.");
        }
    }
}
