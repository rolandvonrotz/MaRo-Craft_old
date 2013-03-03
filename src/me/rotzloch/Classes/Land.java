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
						this.setCorners(50);
					}
					Helper.SendMessageInfo(player, Text.LandBuyed(this.regionName,this.buyPrice));
				} else {
					Helper.SendMessageError(player,Text.LandNotEnoughMoney(this.buyPrice));
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
					this.setCorners(76);
				}
				Helper.PayToTarget(Helper.ServerAccount(), this.player.getName(),
						this.sellPrice);
				Helper.SendMessageInfo(this.player,  Text.LandSelled(this.regionName,this.sellPrice));
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
				Helper.BroadcastMessage(Text.LandBuyWithSign(this.regionName, buyername));
			} else {
				Helper.SendMessageError(player, Text.LandNotEnoughMoney(sellprice));
			}
		} else {
			Helper.SendMessageError(player,Text.LandCanNotBuyYourLand);
		}
	}

	public void AddMember(String playerName) {
		if (this.existAndOwner()) {
			DefaultDomain members = this.region.getMembers();
			if (!members.contains(playerName)) {
				members.addPlayer(playerName);
				this.region.setMembers(members);
				this.save();
				Helper.SendMessageInfo(player, Text.LandAddMember(this.regionName, playerName));
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
				Helper.SendMessageInfo(player, Text.LandRemoveMember(this.regionName, playerName));
			}
		}
	}

	public void Lock() {
		if (this.existAndOwner()) {
			this.setFlag("use", "deny");
			this.save();
			Helper.SendMessageInfo(player, Text.LandLock(this.regionName));
		}
	}

	public void Unlock() {
		if (this.existAndOwner()) {
			this.setFlag("use", "allow");
			this.save();
			Helper.SendMessageInfo(player, Text.LandUnlock(this.regionName));
		}
	}

	public void SpawnMobs(boolean flag, boolean sendMessage, boolean newRegion) {
		if (newRegion || this.existAndOwner()) {
			if (flag) {
				this.setFlag("mob-spawning", "allow");
				if (sendMessage) {
					Helper.SendMessageInfo(player, Text.LandMobsSpawn);
				}
			} else {
				this.setFlag("mob-spawning", "deny");
				if (sendMessage) {
					Helper.SendMessageInfo(player, Text.LandMobsNotSpawn);
				}
			}
		}

	}
	
	public void List(String playerName) {
		if(playerName == "") {
			playerName = this.player.getName();
		}
		Helper.SendMessageInfo(this.player, Text.LandGSList(playerName));
		
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
					Text.LandGS + "'" + this.region.getId() + "'");
			if(this.region.getOwners().toPlayersString().length() > 0) {
				Helper.SendMessageInfo(this.player, Text.LandOwners
						+ this.region.getOwners().toPlayersString());
			}
			if(this.region.getMembers().size() > 0) {
				Helper.SendMessageInfo(this.player, Text.LandMembers
						+ this.region.getMembers().toPlayersString());
			}
			String flags = this.GetFlags();
			if(flags != null) {
				Helper.SendMessageInfo(this.player, flags);
			}
			if(this.region.getOwners().contains(this.player.getName())) {
				Helper.SendMessageError(this.player, Text.LandCanSell(this.sellPrice));
			}
		} else {
			Helper.SendMessageError(this.player, Text.LandCanBuy(this.regionName, this.buyPrice));
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
			Helper.BroadcastMessage(Text.LandSellBySign(this.regionName, player.getName()));
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

	private void setCorners(int cornerID) {
		MarkerTask task = new MarkerTask(this.world, this.chunk, cornerID);
		Helper.StartDelayedTask(task, 1);
	}
}
