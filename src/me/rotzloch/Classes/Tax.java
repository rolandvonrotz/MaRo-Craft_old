package me.rotzloch.Classes;

import org.bukkit.entity.Player;

public class Tax implements Runnable{
	private double taxPerGs = 0.1;
	private long timeTicks = 18000L;
	private int taskID = -1;

	public Tax() {
		this.taxPerGs = Helper.Config().getDouble("config.Land.TaxPerGS");
		this.timeTicks = Helper.Config().getLong("config.Land.TaxTimeSeconds") * 20;
	}
	
	public void StartTax() {
		if(this.taskID != -1) {
			Helper.StopAsyncTask(this.taskID);
		}
		
		if(this.timeTicks < 200){
			this.timeTicks = 200;
		}
		this.taskID = Helper.StartAsyncTask(this, this.timeTicks);
	}

	private void CalcTax(Player player) {
		int countGSPlayer = Helper.CountGS(player);
		if (countGSPlayer > 0 && Helper.HasPlayerAccountAndEnoughBalance(player.getName(), 0)) {
			double price = this.taxPerGs;
			price = countGSPlayer * price;
			price = price * 100;
			price = Math.round(price);
			price = price / 100;
			player.sendMessage(Text.LandTax(price));
			Helper.PayToTarget(player.getName(), Helper.ServerAccount(),
					price);
		}
	}

	@Override
	public void run() {
		for (Player player : Helper.GetOnlinePlayers()) {
			if (!player.hasPermission("marocraft.taxfree")) {
				this.CalcTax(player);
			}
		}		
	}
}
