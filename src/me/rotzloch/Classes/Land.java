package me.rotzloch.Classes;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Land {
	
	private double price = 200;
	private double buyPrice = 0;
	private double sellPrice = 0;
	private Player player;
	private LocalPlayer localPlayer;
	private Chunk chunk;
	private Location pt1;
	private Location pt2;
	private World world;
	private String regionName;
	private ProtectedRegion region;
	private RegionManager rm;
	public CommandSender sender;
	
	public Land(Player player, int x, int z) {
		this.price = Helper.Config().getInt("config.Land.BuyPrice");
		this.player = player;
		this.localPlayer = Helper.getWorldGuard().wrapPlayer(player);
		this.world = player.getWorld();
		this.Init(x, z);
	}
	
	//Public Methods
	public void Buy() {
		if (this.player.hasPermission("marocraft.buy")) {
			if (!this.allreadyExist()) {
				if (Helper.HasPlayerAccountAndEnoughBalance(
						this.player.getName(), buyPrice)) {
					this.setBuyFlagsAndOwner();
					rm.addRegion(region);
					this.save();
					Helper.PayToTarget(player.getName(), Helper.ServerAccount(),
							buyPrice);
					if (!player.hasPermission("marocraft.nocorner")) {
						this.setCorners(89);
					}
					Helper.SendMessageInfo(player, "'" + regionName
							+ "' erfolgreich gekauft.");
					Helper.SendMessageInfo(player, this.buyPrice
							+ " Maros wurden deinem Konto abgezogen!");
				} else {
					Helper.SendMessageError(player,
							"Dieses Grundstück ist zu teuer für dich");
					Helper.SendMessageError(player, "Es kostet: " + buyPrice);
				}
			} else {
				this.Info();
			}
		} else
		{
			Helper.SendMessageNoPermission(this.player);
		}
	}

	public void Sell() {
		if (this.player.hasPermission("marocraft.sell")) {
			if (this.existAndOwner()) {
				this.rm.removeRegion(this.regionName);
				this.save();
				if (!player.hasPermission("marocraft.nocorner")) {
					//this.removeCorner(89);
				}
				Helper.PayToTarget(Helper.ServerAccount(), this.player.getName(),
						this.sellPrice);
				Helper.SendMessageInfo(this.player, "'" + this.regionName
						+ "' erfolgreich verkauft.");
				Helper.SendMessageInfo(player, this.sellPrice
						+ " Maros wurden deinem Konto gutgeschrieben.");
			}
		}else
		{
			Helper.SendMessageNoPermission(this.player);
		}
	}

	public void ChangeOwner(PlayerInteractEvent event, String buyername,
			String playername, int sellprice, Player player) {

		if (!buyername.equalsIgnoreCase(playername)) {
			if (Helper.HasPlayerAccountAndEnoughBalance(buyername, sellprice)) {
				DefaultDomain owners = this.region.getOwners();
				owners.removePlayer(playername);
				owners.addPlayer(buyername);
				this.region.setOwners(owners);
				this.save();
				Helper.PayToTarget(buyername, playername, sellprice);
				event.getClickedBlock().breakNaturally();
				Helper.BroadcastMessage(
						"Grundstück '" + this.regionName + "' wurde von '"+buyername+"' gekauft.");
			} else {
				Helper.SendMessageError(player, "Du hast zu wenig Maro's");
			}
		} else {
			Helper.SendMessageError(player,
					"Du kannst deine eigenen Grundstücke nicht kaufen.");
		}
	}

	public void AddMember(String playerName) {
		if (this.existAndOwner()) {
			DefaultDomain members = this.region.getMembers();
			if (!members.contains(playerName)) {
				members.addPlayer(playerName);
				this.region.setMembers(members);
				this.save();
				Helper.SendMessageInfo(player, "Spieler '" + playerName
						+ "' dem Grundstück '" + this.regionName
						+ "' hinzugefügt.");
			}
		}
	}

	public void RemoveMember(String playerName) {
		if (this.existAndOwner()) {
			DefaultDomain members = this.region.getMembers();
			if (members.contains(playerName)) {
				members.removePlayer(playerName);
				this.region.setMembers(members);
				this.save();
				Helper.SendMessageInfo(player, "Spieler '" + playerName
						+ "' vom Grundstück '" + this.regionName
						+ "' entfernt.");
			}
		}
	}

	public void Lock() {
		if (this.existAndOwner()) {
			this.setFlag("use", "deny");
			this.save();
			Helper.SendMessageInfo(player, "Grundstück '" + this.regionName
					+ "' wurde gesperrt.");
		}
	}

	public void Unlock() {
		if (this.existAndOwner()) {
			this.setFlag("use", "allow");
			this.save();
			Helper.SendMessageInfo(player, "Grundstück '" + this.regionName
					+ "' wurde entsperrt.");
		}
	}

	public void SpawnMobs(boolean flag, boolean sendMessage, boolean newRegion) {
		if (newRegion || this.existAndOwner()) {
			if (flag) {
				this.setFlag("mob-spawning", "allow");
				if (sendMessage) {
					Helper.SendMessageInfo(player, "Mobs können nun spawnen");
				}
			} else {
				this.setFlag("mob-spawning", "deny");
				if (sendMessage) {
					Helper.SendMessageInfo(player,
							"Mobs können nun nicht spawnen");
				}
			}
		}

	}
	
	public void List(String playerName) {
		if(playerName == "") {
			playerName = this.player.getName();
		}
		Helper.SendMessageInfo(this.player, "Folgende GS besitzt "+playerName+":");
		Helper.SendMessageInfo(this.player, "-----------------------");
		
		for(World world : Helper.GetWorlds()) {
			RegionManager regionm = Helper.getWorldGuard().getRegionManager(world);
			for (Entry<String, ProtectedRegion> item : regionm.getRegions().entrySet()) {
				if(item.getValue().isOwner(playerName)) {
					Helper.SendMessageInfo(this.player, "- "+ world.getName()+" -> " + item.getValue().getId());
				}
			}
		}
	}

	public void Info() {
		if (this.allreadyExist()) {
			Helper.SendMessageInfo(this.player,
					"Grundstück '" + this.region.getId() + "'");
			if(this.region.getOwners().toPlayersString().length() > 0) {
				Helper.SendMessageInfo(this.player, "Besitzer: "
						+ this.region.getOwners().toPlayersString());
			}
			if(this.region.getMembers().size() > 0) {
				Helper.SendMessageInfo(this.player, "Mitglieder: "
						+ this.region.getMembers().toPlayersString());
			}
			String flags = this.GetFlags();
			if(flags != null) {
				Helper.SendMessageInfo(this.player, flags);
			}
			if(this.region.getOwners().contains(this.player.getName())) {
				Helper.SendMessageError(this.player, "Du kannst es verkaufen für "
						+ this.sellPrice + " Maros.");
			}
		} else {
			Helper.SendMessageError(this.player, "Grundstück '"
					+ this.regionName + "' gehört niemandem.");
			Helper.SendMessageInfo(this.player, "Du kannst es kaufen für "
					+ this.buyPrice + " Maros.");
		}
	}

	public void RemoveFlag(String flagName) {
		Flag<?> foundFlag = this.getFlag(flagName);
		this.region.getFlags().remove(foundFlag);
	}

	public void SellBySign(SignChangeEvent event) {
		if (this.existAndOwner()) {
			event.setLine(0, ChatColor.DARK_BLUE + "[L-SELL]");
			event.setLine(2, this.regionName);
			event.setLine(3, player.getName());
			Helper.BroadcastMessage("Grundstück '" + this.regionName
					+ "' wird von '"+player.getName()+"' zum Verkauf angeboten.");
		}
	}
	public boolean existAndOwner() {
		if (this.allreadyExist()) {
			if (this.region.getOwners().contains(this.localPlayer)) {
				return true;
			}
		}
		this.Info();
		return false;
	}

	//Private Methods
	// Initialisierung des Grundstückes
	private void Init(int x, int z) {
		this.chunk = this.world.getChunkAt(x, z);
		this.regionName = "region_" + this.chunk.getX() + "_"
				+ this.chunk.getZ();
		this.rm = Helper.getWorldGuard().getRegionManager(this.world);

		Block b1 = this.chunk.getBlock(0, 0, 0);
		Block b2 = this.chunk.getBlock(15, 255, 15);

		pt1 = b1.getLocation();
		pt2 = b2.getLocation();
		this.setRegion();
		this.setPrices();
	}

	// Setzen der Grundstückpreise (Kaufen und Verkaufen)
	private void setPrices() {
		if (!this.player.hasPermission("marocraft.free")) {
			int count = Helper.CountGS(this.player);
			if (count == 0) {
				this.buyPrice = 0;
			} else {
				count -= 1;

				this.buyPrice = this.price
						+ (count * (Helper.Config()
								.getInt("config.Land.AddBuyPricePerGS")));
			}
			if (count < 1) {
				this.sellPrice = 0;
			} else {
				count -= 1;

				this.sellPrice = this.price
						+ (count * (Helper.Config()
								.getInt("config.Land.AddBuyPricePerGS")));

				this.sellPrice = (sellPrice * 80) / 100;
			}
		} else {
			this.buyPrice = 0;
			this.sellPrice = 0;
		}
	}

	// Falls vorhanden protected Region aus Worldguard laden, ansonsten
	// Selektion als neue Region setzen
	private void setRegion() {
		if (this.allreadyExist()) {
			this.region = this.rm.getRegion(this.regionName);
		} else {
			CuboidSelection sel = new CuboidSelection(world, pt1, pt2);
			this.region = this.getProtectedRegion(sel);
		}
	}

	// Protected Region zurückgeben anhand der Selektion
	private ProtectedRegion getProtectedRegion(Selection sel) {
		if (sel instanceof CuboidSelection) {
			BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
			BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
			return new ProtectedCuboidRegion(this.regionName, min, max);
		} else {
			return null;
		}
	}

	// Prüfen ob ein Grundstück bereits verkauft ist.
	private boolean allreadyExist() {
		return this.rm.hasRegion(this.regionName);
	}

	private void setBuyFlagsAndOwner() {
		DefaultDomain owners = new DefaultDomain();
		owners.addPlayer(localPlayer);
		region.setOwners(owners);
		this.setFlag("creeper-explosion", "deny");
		this.setFlag("tnt", "deny");
		this.setFlag("enderman-grief", "deny");
		this.setFlag("use", "allow");
		this.SpawnMobs(false, false, true);
		this.region.setPriority(5);
	}

	// Worldguard Flags
	private Flag<?> getFlag(String flagName) {
		for (Flag<?> flag : DefaultFlag.getFlags()) {
			if (flag.getName().replace("-", "")
					.equalsIgnoreCase(flagName.replace("-", ""))) {
				return flag;
			}
		}
		return null;
	}

	private void setFlag(String flagName, String flagValue) {
		Flag<?> foundFlag = this.getFlag(flagName);
		if (foundFlag != null) {
			this.setFlagToRegion(foundFlag, flagValue);
		}
	}

	private <V> void setFlagToRegion(Flag<V> flag, String flagValue) {
		if (flag != null) {
			try {
				this.region.setFlag(flag, flag.parseInput(
						Helper.getWorldGuard(), this.sender, flagValue));
			} catch (CommandException e) {
				e.printStackTrace();
			} catch (InvalidFlagFormat e) {
				e.printStackTrace();
			}
		}
	}

	private void save() {
		try {
			this.rm.save();
		} catch (ProtectionDatabaseException e) {
			e.printStackTrace();
		}
	}

	private String GetFlags() {
		String flags = "Flags: ";
		Flag<?> flagUse = this.getFlag("use");
		Object valUse = null;
		if(flagUse != null) {
			valUse = this.region.getFlag(flagUse);
			if(valUse != null) {
				flags += "Locked->";
				if(String.valueOf(valUse).equalsIgnoreCase("deny")) {
					flags += "Ja";
				} else {
					flags += "Nein";
				}
			}
		}
		
		Flag<?> flagMobs = this.getFlag("mob-spawning");
		Object valMobs = null;
		if(flagMobs != null) {
			valMobs = this.region.getFlag(flagMobs);
			if(valMobs != null) {
				if(flagUse != null && valUse != null) {
					flags += ", ";
				}
				flags += "Mobs->";				
				if(String.valueOf(valMobs).equalsIgnoreCase("deny")) {
					flags += "Nein";
				} else {
					flags += "Ja";
				}
			}
		}
		if(flags.equalsIgnoreCase("Flags: ")) {
			flags = null;
		}
		return flags;
	}

	// setCorners
	private void setCorners(int cornerID) {
	// int cornerID = 89;
		for (int x = 0; x <= 15; x = x + 15)
			for (int z = 0; z <= 15; z = z + 15) {
				int y = this.world.getMaxHeight() - 2;

				Block block = this.chunk.getBlock(x, y, z);
				while (y >= 0) {
					// Do not stack cornerID Blocks
					if (block.getTypeId() == cornerID)
						break;

					switch (block.getType()) {
					case LEAVES: // Fall through
					case AIR:
						y--;
						break;

					case SAPLING: // Fall through
					case LONG_GRASS: // Fall through
					case DEAD_BUSH: // Fall through
					case YELLOW_FLOWER: // Fall through
					case RED_ROSE: // Fall through
					case BROWN_MUSHROOM: // Fall through
					case RED_MUSHROOM: // Fall through
					case SNOW:
						block.setTypeId(cornerID);
						y = -1;
						break;

					case BED_BLOCK: // Fall through
					case POWERED_RAIL: // Fall through
					case DETECTOR_RAIL: // Fall through
					case RAILS: // Fall through
					case STONE_PLATE: // Fall through
					case WOOD_PLATE:
						block.getRelative(0, 2, 0).setTypeId(cornerID);
						y = -1;
						break;

					default:
						block.getRelative(0, 1, 0).setTypeId(cornerID);
						y = -1;
						break;
					}

					block = block.getRelative(0, -1, 0);
				}
			}
	}
}
