// Copyright (C) 2012 Rotzloch (roland.vonrotz@maroweb.org)
package me.rotzloch.Commands;

import java.util.Set;

import me.rotzloch.Classes.Farm;
import me.rotzloch.Classes.Helper;
import me.rotzloch.Classes.Text;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class FarmCommandExecutor implements CommandExecutor {

	private static enum Action {
		ADD, CHANGEAREA, CHANGECHEST, CHANGEITEM, REMOVE, LIST, HELP
	}

	public FarmCommandExecutor() {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (command.getName().equalsIgnoreCase("farm")) {
			if (sender instanceof Player) {
				Action action;
				Player player = (Player) sender;

				try {
					action = Action.valueOf(args[0].toUpperCase());
				} catch (Exception notEnum) {
					Helper.Help(player, "farm");
					return true;
				}
				
				Farm area = new Farm(player);
				
				switch (action) {
				case ADD:
					if(player.hasPermission("marocraft.farmonator.add")) {
						area.setTruhe(this.getChest(player, args[2], args[3], args[4]));
						String itemID = "-1";
						if(args.length == 6) {
							itemID = args[5];
						}
						area.AddNewFarm(args[1],player, itemID);
					} else {
						player.sendMessage(Text.NoPermission);
					}
					break;
				case CHANGEAREA:
					if(player.hasPermission("marocraft.farmonator.change.area")) {
						area.ChangeFarmArea(args[1], player);
					} else {
						player.sendMessage(Text.NoPermission);
					}
					break;
				case CHANGECHEST:
					if(player.hasPermission("marocraft.farmonator.change.chest")) {
						area.ChangeFarmChest(args[1], player, this.getChest(player, args[2], args[3], args[4]));
					} else {
						player.sendMessage(Text.NoPermission);
					}
					break;
				case CHANGEITEM:
					if(player.hasPermission("marocraft.farmonator.change.item")) {
						area.ChangeFarmItem(args[1], player, args[2]);
					} else {
						player.sendMessage(Text.NoPermission);
					}						
					break;
				case REMOVE:
					if(player.hasPermission("marocraft.farmonator.remove")) {
						area.DelFarm(args[1], player);
					} else {
						player.sendMessage(Text.NoPermission);
					}
					break;
				case LIST:
					if(player.hasPermission("marocraft.farmonator.list")) {
						this.getFarmAreas(player);	
					} else {
						player.sendMessage(Text.NoPermission);
					}
					break;
				case HELP : 
					if(player.hasPermission("marocraft.farmonator.help")) {
						Helper.Help(player, "farm");
					} else {
						player.sendMessage(Text.NoPermission);
					}
				}

			} else {
				Helper.LogInfo("Must call by Player");
			}
			return true;
		}
		return false;
	}

	public void getFarmAreas(Player player) {
		Set<String> areas = Helper.FarmDB().keySet();
		player.sendMessage(Text.FarmList);
		for (String area : areas) {
			player.sendMessage(ChatColor.BLUE + area);
		}
	}
	
	private Chest getChest(Player player, String x, String y, String z) {
        Block exp = player.getWorld().getBlockAt(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
        Chest truhe = null;
        if (exp != null && exp.getType() == Material.CHEST) {
            truhe = (Chest) exp.getState();
        } else {
            player.sendMessage(Text.FarmChestNotFound(x,y,z));
        }
        return truhe;
    }
}
