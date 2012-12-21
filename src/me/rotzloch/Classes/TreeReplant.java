package me.rotzloch.Classes;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class TreeReplant implements Runnable {
	public Block block;
	public byte data;
	
	public TreeReplant(Block importBlock, byte importData) {
		this.block = importBlock;
		this.data = importData;
	}
	
	@Override
	public void run() {
		this.block.setType(Material.SAPLING);
		this.block.setData(data);
	}

}
