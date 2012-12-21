package me.rotzloch.Classes;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;

public class Farm {
	private BlockVector min;
	private BlockVector max;
	private String world;
	private String playername;

	private String id;
	private Chest truhe;
	private int itemid;
	private short damage;
	
	private String table = "farmonator";

	public Farm(Player player) {
		this.playername = player.getName();
		this.world = player.getWorld().getName();
		if(Helper.getWorldEdit().getSelection(player) != null) {
			List<BlockVector> points = new ArrayList<BlockVector>();
			points.add(Helper.getMinimumWedit(player));
			points.add(Helper.getMaximumWedit(player));
			this.setMinMaxPoints(points);
		}
	}

	public Farm(String player, String world, String id,
			Chest truhe, String item) {
		this.playername = player;
		this.world = world;
		this.id = id;
		this.truhe = truhe;
		this.setItem(item);
	}

	public Farm(ResultSet result) {
		try {
			this.playername = result.getString(3);
			this.world = result.getString(2);
			this.id = result.getString(1);
			
			Vector v1 = new Vector(result.getDouble(6),result.getDouble(7),result.getDouble(8));
			Vector v2 = new Vector(result.getDouble(9),result.getDouble(10),result.getDouble(11));
			BlockVector min = Vector.getMinimum(v1, v2).toBlockVector();
	        BlockVector max = Vector.getMaximum(v1, v2).toBlockVector();
	        this.setMinMax(min, max);
	        
			int itemId = result.getInt(4);
	        String damage = ""+result.getInt(5);
	        String item = ""+itemId;
	        if(damage != "-1") {
	            item += ":"+damage;
	        } 
	        this.setItem(item);
	        
	        World world = Helper.GetWorld(this.world);
	        Block exp = world.getBlockAt(result.getInt(12),result.getInt(13),result.getInt(14));
	        Chest truhe;
	        if (exp.getType() == Material.CHEST) {
	            truhe = (Chest) exp.getState();
	        } else {
	        	return;
	        }
	        this.truhe = truhe;
		} catch(Exception ex) {
			Helper.LogError(ex.getMessage());
			return;
		}
	}
	
	private void setMinMaxPoints(List<BlockVector> points) {
		int minX = ((BlockVector) points.get(0)).getBlockX();
		int minY = ((BlockVector) points.get(0)).getBlockY();
		int minZ = ((BlockVector) points.get(0)).getBlockZ();
		int maxX = minX;
		int maxY = minY;
		int maxZ = minZ;

		for (Vector v : points) {
			int x = v.getBlockX();
			int y = v.getBlockY();
			int z = v.getBlockZ();

			if (x < minX) {
				minX = x;
			}
			if (y < minY) {
				minY = y;
			}
			if (z < minZ) {
				minZ = z;
			}

			if (x > maxX) {
				maxX = x;
			}
			if (y > maxY) {
				maxY = y;
			}
			if (z > maxZ) {
				maxZ = z;
			}
		}

		this.min = new BlockVector(minX, minY, minZ);
		this.max = new BlockVector(maxX, maxY, maxZ);
	}

	private boolean idExist(String id) {
		return Helper.FarmDB().containsKey(id);
	}

	private boolean insertDatabase(){
		String[] columns = {"(id,","world,","player,","itemId,","itemDamage,","minPointX,","minPointY,","minPointZ,","maxPointX,","maxPointY,","maxPointZ,","truheX,","truheY,","truheZ)"};
		String[] values = {"","","","","","","","","","","","","",""};
		values[0] += "('"+this.getId()+"',";
		values[1] += "'"+this.getWorldName()+"',";
		values[2] += "'"+this.getPlayerName()+"',";
		values[3] += "'"+this.getItemId()+"',";
		values[4] += "'"+this.getDamage()+"',";
		values[5] += "'"+this.getMinimumPoint().getX()+"',";
		values[6] += "'"+this.getMinimumPoint().getY()+"',";
		values[7] += "'"+this.getMinimumPoint().getZ()+"',";
		values[8] += "'"+this.getMaximumPoint().getX()+"',";
		values[9] += "'"+this.getMaximumPoint().getY()+"',";
		values[10] += "'"+this.getMaximumPoint().getZ()+"',";
		values[11] += "'"+this.getTruhe().getLocation().getX()+"',";
		values[12] += "'"+this.getTruhe().getLocation().getY()+"',";
		values[13] += "'"+this.getTruhe().getLocation().getZ()+"')";
		
		String query = "INSERT INTO "+table+" ";
		for(String column:columns){
			query += column;
		}
		query += " VALUES ";
		for(String value:values){
			query += value;
		}
		return this.executeDatabaseQuery(query);
	}
	
	private boolean updateDatabase(Farm farm){
		String query = "UPDATE "+table+" SET ";
		for(String value:farm.getValues()){
			query += value;
		}
		query += " WHERE id = '"+farm.getId()+"'";
		return this.executeDatabaseQuery(query);
	}
	
	private boolean executeDatabaseQuery(String query){
		try {
			ResultSet result = null;
			if(Helper._farmonatorMysql != null){
				result = Helper._farmonatorMysql.query(query);
			} else if (Helper._farmonatorSQLLite != null) {
				result = Helper._farmonatorSQLLite.query(query);
			}
			if(result != null){
				result.close();
			}
		} catch(Exception e){
			Helper.LogError(e.getMessage());
			return false;
		}
		return true;
	}
	
	public void AddNewFarm(String id, Player player, String item) {
		try{
			this.setItem(item);
	
			if (!this.idExist(id)) {
				if (!this.isThereAExistFarmArea(this)) {
					this.id = id;
					if(this.insertDatabase()){
						Helper.setFarm(id, this);
						player.sendMessage(ChatColor.GREEN
								+ "[Farmonator] Farm '"+id+"' wurde gespeichert!");
					} else {
						player.sendMessage(ChatColor.RED
								+ "[Farmonator] Beim Speichern ist ein Fehler aufgetreten!");
						Helper.LogError("Fehler beim Speichern in Datenbank");
					}
					
				} else {
					player.sendMessage(ChatColor.GREEN
							+ "[Farmonator] Deine Farm überschneidet eine bestehende Farm!");
				}
			} else {
				player.sendMessage(ChatColor.GREEN
						+ "[Farmonator] Dieser Name existiert bereits!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			Helper.LogError(e.getMessage());
		}
	}

	public void ChangeFarmArea(String id, Player player) {
		if (this.idExist(id)) {
			Farm editFarm = Helper.FarmDB().get(id);
			if(editFarm == null) {
				Helper.LogError("Farm not found");
			} else {
				if (editFarm.getPlayerName().equalsIgnoreCase(player.getName())) {
					List<BlockVector> points = new ArrayList<BlockVector>();
					points.add(Helper.getMinimumWedit(player));
					points.add(Helper.getMaximumWedit(player));
					editFarm.setMinMaxPoints(points);
	
					if (!this.isThereAExistFarmArea(editFarm)) {
						if(this.updateDatabase(editFarm)){
							Helper.setFarm(id, editFarm);
							player.sendMessage(ChatColor.GREEN
									+ "[Farmonator] Farm '"+id+"' wurde gespeichert!");
						} else {
							player.sendMessage(ChatColor.RED
									+ "[Farmonator] Beim Speichern ist ein Fehler aufgetreten!");
							Helper.LogError("Fehler beim Speichern in Datenbank");
						}
					} else {
						player.sendMessage(ChatColor.GREEN
								+ "[Farmonator] Deine Farm überschneidet eine bestehende Farm!");
					}
				} else {
					player.sendMessage(ChatColor.GREEN
							+ "[Farmonator] Diese Farm gehört nicht dir.");
				}
			}
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "[Farmonator] Diese Farm existiert nicht.");
		}
	}

	public void ChangeFarmItem(String id, Player player, String item) {
		if (this.idExist(id)) {
			Farm editFarm = Helper.FarmDB().get(id);
			if (editFarm.getPlayerName().equalsIgnoreCase(player.getName())) {
				editFarm.setItem(item);
				if (!this.isThereAExistFarmArea(editFarm)) {
					if(this.updateDatabase(editFarm)){
						Helper.setFarm(id, editFarm);
						player.sendMessage(ChatColor.GREEN
								+ "[Farmonator] Farm '"+id+"' wurde gespeichert!");
					} else {
						player.sendMessage(ChatColor.RED
								+ "[Farmonator] Beim Speichern ist ein Fehler aufgetreten!");
						Helper.LogError("Fehler beim Speichern in Datenbank");
					}
				} else {
					player.sendMessage(ChatColor.GREEN
							+ "[Farmonator] Deine Farm überschneidet eine bestehende Farm!");
				}
			} else {
				player.sendMessage(ChatColor.GREEN
						+ "[Farmonator] Diese Farm gehört nicht dir.");
			}
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "[Farmonator] Diese Farm existiert nicht.");
		}
	}

	public void ChangeFarmChest(String id, Player player, Chest truhe) {
		if (this.idExist(id)) {
			Farm editFarm = Helper.FarmDB().get(id);
			if (editFarm.getPlayerName().equalsIgnoreCase(player.getName())) {
				editFarm.setTruhe(truhe);
				if (!this.isThereAExistFarmArea(editFarm)) {
					if(this.updateDatabase(editFarm)){
						Helper.setFarm(id, editFarm);
						player.sendMessage(ChatColor.GREEN
								+ "[Farmonator] Farm '"+id+"' wurde gespeichert!");
					} else {
						player.sendMessage(ChatColor.RED
								+ "[Farmonator] Beim Speichern ist ein Fehler aufgetreten!");
						Helper.LogError("Fehler beim Speichern in Datenbank");
					}
				} else {
					player.sendMessage(ChatColor.GREEN
							+ "[Farmonator] Deine Farm überschneidet eine bestehende Farm!");
				}
			} else {
				player.sendMessage(ChatColor.GREEN
						+ "[Farmonator] Diese Farm gehört nicht dir.");
			}
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "[Farmonator] Diese Farm existiert nicht.");
		}
	}

	public void DelFarm(String id, Player player) {
		if (this.idExist(id)) {
			if (Helper.FarmDB().get(id).getPlayerName().equalsIgnoreCase(player.getName())) {
				Helper.FarmDB().remove(id);
				String query = "DELETE FROM farmonator WHERE id = '"+id+"'";
				try {
					ResultSet result = null;
					if(Helper._farmonatorMysql != null){
						result = Helper._farmonatorMysql.query(query);
					} else if(Helper._farmonatorSQLLite != null){
						result = Helper._farmonatorSQLLite.query(query);
					}
					if(result != null){
						result.close();
					}
				} catch(Exception e){
					Helper.LogError(e.getMessage());
				}
				
				player.sendMessage(ChatColor.GREEN
						+ "[Farmonator] Farm '"+id+"' wurde entfernt!");
			} else {
				player.sendMessage(ChatColor.GREEN
						+ "[Farmonator] Diese Farm gehört nicht dir.");
			}
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "[Farmonator] Diese Farm existiert nicht.");
		}
	}

	public void setMinMax(BlockVector pt1, BlockVector pt2) {
		this.min = pt1;
		this.max = pt2;
	}

	public boolean isThereAExistFarmArea(Farm area) {
		boolean exist = false;

		double x1 = area.getMinimumPoint().getX();
		double y1 = area.getMinimumPoint().getY();
		double z1 = area.getMinimumPoint().getZ();

		double x2 = area.getMaximumPoint().getX();
		double y2 = area.getMaximumPoint().getY();
		double z2 = area.getMaximumPoint().getZ();

		for (Iterator<Entry<String, Farm>> it = Helper.FarmDB().entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Farm> entry = (Map.Entry<String, Farm>) it.next();
			Farm areaExist = (Farm) entry.getValue();

			if (area.getId() == areaExist.getId()) {
				continue;
			}
			double x3 = areaExist.getMinimumPoint().getX();
			double y3 = areaExist.getMinimumPoint().getY();
			double z3 = areaExist.getMinimumPoint().getZ();

			double x4 = areaExist.getMaximumPoint().getX();
			double y4 = areaExist.getMaximumPoint().getY();
			double z4 = areaExist.getMaximumPoint().getZ();

			if ((x1 >= x3 && x1 <= x4) || (x2 >= x3 && x2 <= x4)
					|| (x3 >= x1 && x3 <= x2) || (x4 >= x1 && x4 <= x2)) {
				if ((y1 >= y3 && y1 <= y4) || (y2 >= y3 && y2 <= y4)
						|| (y3 >= y1 && y3 <= y2) || (y4 >= y1 && y4 <= y2)) {
					if ((z1 >= z3 && z1 <= z4) || (z2 >= z3 && z2 <= z4)
							|| (z3 >= z1 && z3 <= z2) || (z4 >= z1 && z4 <= z2)) {
						if (((areaExist.getItemId() == area.getItemId() && areaExist
								.getDamage() == area.getDamage()))
								|| areaExist.getItemId() == -1
								|| area.getItemId() == -1) {
							exist = true;
						}
					}
				}
			}
		}
		return exist;
	}

	public String getId() {
		return this.id;
	}

	public BlockVector getMinimumPoint() {
		return this.min;
	}

	public BlockVector getMaximumPoint() {
		return this.max;
	}

	public Chest getTruhe() {
		return this.truhe;
	}

	public String getPlayerName() {
		return this.playername;
	}

	public String getWorldName() {
		return this.world;
	}

	public int getItemId() {
		return this.itemid;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setTruhe(Chest truhe) {
		this.truhe = truhe;
	}

	public void setItem(String item) {
		if (item == null) {
			this.itemid = -1;
			this.damage = -1;
		} else {

			String[] itemTypeDmg = item.split(":");

			if (itemTypeDmg.length == 2) {
				this.itemid = Integer.parseInt(itemTypeDmg[0]);
				this.damage = Short.parseShort(itemTypeDmg[1]);
			} else if (itemTypeDmg.length == 1) {
				this.itemid = Integer.parseInt(itemTypeDmg[0]);
				this.damage = -1;
			}
		}
	}

	public short getDamage() {
		return this.damage;
	}
	
	public String[] getValues() {
		String[] values = {"","","","","","","","","","","","",""};
		values[0] += "world='"+this.world+"',";
		values[1] += "player='"+this.playername+"',";
		values[2] += "itemId='"+this.itemid+"',";
		values[3] += "itemDamage='"+this.damage+"',";
		values[4] += "minPointX='"+this.getMinimumPoint().getX()+"',";
		values[5] += "minPointY='"+this.getMinimumPoint().getY()+"',";
		values[6] += "minPointZ='"+this.getMinimumPoint().getZ()+"',";
		values[7] += "maxPointX='"+this.getMaximumPoint().getX()+"',";
		values[8] += "maxPointY='"+this.getMaximumPoint().getY()+"',";
		values[9] += "maxPointZ='"+this.getMaximumPoint().getZ()+"',";
		values[10] += "truheX='"+this.truhe.getLocation().getX()+"',";
		values[11] += "truheY='"+this.truhe.getLocation().getY()+"',";
		values[12] += "truheZ='"+this.truhe.getLocation().getZ()+"'";
		return values;
	}
}
