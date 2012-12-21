package me.rotzloch.listener;

import java.util.List;

import me.rotzloch.Classes.Helper;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ItemStackerListener implements Listener{
	@EventHandler
	public void OnItemSpawn(ItemSpawnEvent event) {
		int radius = Helper.ItemStackerRadius();
		if (Helper.ItemStackerRadius() <= 0 || event.getEntityType() != EntityType.DROPPED_ITEM) {
			return;
		}
		Item newEntity = event.getEntity();
		int maxSize = Helper.ItemStackerItemsPerStack();

		List<Entity> entityList = newEntity.getNearbyEntities(radius, radius, radius);

		boolean normal = Helper.ItemStackerNormal();
		for (int i = 0; i < entityList.size(); i++) {
			if (entityList.get(i) instanceof Item) {
				Item curEntity = (Item) entityList.get(i);
				if (!curEntity.isDead()) {
					if (curEntity.getItemStack().getType().toString() == newEntity.getItemStack().getType().toString()) {
						if (curEntity.getItemStack().getData().getData() == newEntity.getItemStack().getData().getData()) {
							if (curEntity.getItemStack().getDurability() == newEntity.getItemStack().getDurability()) {
								if (Math.abs(curEntity.getLocation().getX() - newEntity.getLocation().getX()) <= radius
										&& Math.abs(curEntity.getLocation().getY() - newEntity.getLocation().getY()) <= radius
										&& Math.abs(curEntity.getLocation().getZ() - newEntity.getLocation().getZ()) <= radius) {

									int newAmount = newEntity.getItemStack().getAmount();
									int curAmount = curEntity.getItemStack().getAmount();

									if (normal) {
										int more = Math.min(curAmount, maxSize - newAmount);
										newAmount += more;
										curAmount -= more;
										newEntity.getItemStack().setAmount(newAmount);
										curEntity.getItemStack().setAmount(curAmount);
										if (curAmount <= 0) {
											curEntity.remove();
										}
									} else {
										int more = Math.min(newAmount, maxSize - curAmount);
										curAmount += more;
										newAmount -= more;
										curEntity.getItemStack().setAmount(curAmount);
										newEntity.getItemStack().setAmount(newAmount);
										if (newAmount <= 0) {
											event.setCancelled(true);
										}
									}
									return;
								}
							}
						}
					}
				}
			}
		}
	}
}
