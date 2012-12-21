// Copyright (C) 2012 Rotzloch (roland.vonrotz@maroweb.org)
package me.rotzloch.listener;

import java.util.Iterator;
import java.util.Map;

import me.rotzloch.Classes.Farm;
import me.rotzloch.Classes.Helper;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldedit.BlockVector;

public class FarmListener implements Listener {
	
	public FarmListener() {
	}

	@EventHandler
    public void blockInFarmArea(ItemSpawnEvent event) {
		this.collectItems(event);
    }
	
    private void collectItems(EntityEvent entityEvent) {
		Item event;
		if (entityEvent.getEntity() instanceof Item) {
			event = (Item) entityEvent.getEntity();
			double x = event.getLocation().getX();
			double y = event.getLocation().getY();
			double z = event.getLocation().getZ();

			Farm area = this.itemInFarmArea(event, x, y, z);
			if (area != null && area.getTruhe() != null) {
				Chest truhe = area.getTruhe();
				ItemStack blockNew = event.getItemStack();
				for (Iterator<ItemStack> iter = truhe.getInventory().iterator(); iter
						.hasNext();) {
					ItemStack item = iter.next();
					if (item != null && item.getType() != Material.AIR) {
						if (item.getTypeId() == blockNew.getTypeId()
								&& item.getDurability() == blockNew
										.getDurability()
								&& item.getAmount() <= item.getMaxStackSize()) {
							if (item.getAmount() + blockNew.getAmount() <= item
									.getMaxStackSize()) {
								item.setAmount(item.getAmount()
										+ blockNew.getAmount());

								blockNew.setAmount(0);
								event.remove();
								break;
							} else {
								int amount = item.getMaxStackSize()
										- item.getAmount();
								item.setAmount(item.getMaxStackSize());

								blockNew.setAmount(blockNew.getAmount()
										- amount);
								event.getItemStack().setAmount(
										blockNew.getAmount());
							}
						}
					}
				}

				if (blockNew.getAmount() > 0) {
					int freeSlot = truhe.getInventory().firstEmpty();
					if (freeSlot != -1) {
						truhe.getInventory().setItem(freeSlot, blockNew);
						event.remove();
					}
				} else {
					event.remove();
				}
			}
		}
	}
    
    private Farm itemInFarmArea(Item event, double x, double y, double z) {
        Farm area = null;
        int itemId = event.getItemStack().getTypeId();
        for (Map.Entry<String, Farm> entry : Helper.FarmDB().entrySet()) {
            BlockVector min = ((Farm) entry.getValue()).getMinimumPoint();
            BlockVector max = ((Farm) entry.getValue()).getMaximumPoint();

            if (min.getX() <= x && min.getY() <= y && min.getZ() <= z
                    && max.getX() >= x && max.getY() >= y && max.getZ() >= z) {

                area = (Farm) entry.getValue();

                if (area.getItemId() == -1 || area.getItemId() == itemId) {
                    
                    if(area.getDamage() == -1 || area.getDamage() == event.getItemStack().getDurability()) {
                        return area;
                    } else {
                        area = null;
                    }
                } else {
                    area= null;
                }
            }
        }
        return area;
    }
}
