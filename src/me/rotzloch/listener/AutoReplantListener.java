package me.rotzloch.listener;

import me.rotzloch.Classes.Helper;
import me.rotzloch.Classes.TreeReplant;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class AutoReplantListener  implements Listener {
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if(!event.isCancelled()) {
			Block block = event.getBlock();
			if (block.getType() == Material.LOG) {
				this.plantNewLog(block, event.getPlayer());
			}
		}
	}

	private synchronized void plantNewLog(Block blockOld, Player player) {
		byte data = blockOld.getData();

		Block blockUnderWood = blockOld.getRelative(BlockFace.DOWN);
		if (blockUnderWood.getType() == Material.DIRT) {
			Runnable t = new TreeReplant(blockOld, data);
			Helper.StartDelayedTask(t, Helper.Config().getInt("config.AutoReplant.ReplantTicksWait"));
		}
	}
}
