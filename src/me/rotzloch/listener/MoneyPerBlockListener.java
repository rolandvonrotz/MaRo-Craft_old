package me.rotzloch.listener;

import me.rotzloch.Classes.Helper;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;


public class MoneyPerBlockListener implements Listener{
	@EventHandler
	public void OnBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if(player.getGameMode() == GameMode.SURVIVAL && !event.isCancelled()) {
			Material eventType = event.getBlock().getType();
			if(eventType != Material.AIR && eventType != Material.BED && eventType != Material.BOAT &&
			   eventType != Material.SIGN && eventType != Material.SNOW && eventType != Material.TORCH && 
			   eventType != Material.REDSTONE && eventType != Material.REDSTONE_WIRE) {
				if(Helper._blocksBreaked.containsKey(player.getName())){
					Helper._blocksBreaked.put(player.getName(), Helper._blocksBreaked.get(player.getName())+1);
				} else {
					Helper._blocksBreaked.put(player.getName(), 1);
				}
			}
		}
	}
}
