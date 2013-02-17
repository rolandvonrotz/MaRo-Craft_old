package me.rotzloch.main;

import me.rotzloch.Classes.Helper;
import me.rotzloch.Classes.Metrics;
import me.rotzloch.Commands.FarmCommandExecutor;
import me.rotzloch.Commands.LandCommandExecutor;
import me.rotzloch.listener.AutoReplantListener;
import me.rotzloch.listener.FarmListener;
import me.rotzloch.listener.ItemStackerListener;
import me.rotzloch.listener.LandListener;
import me.rotzloch.listener.MoneyPerBlockListener;
import me.rotzloch.listener.RewardListener;

import org.bukkit.plugin.java.JavaPlugin;

public class MaRoCraft extends JavaPlugin {
	
	@Override
	public void onDisable() {
		if(Helper._farmonatorMysql != null){
			Helper._farmonatorMysql.close();
		}
		if(Helper._farmonatorSQLLite != null){
			Helper._farmonatorSQLLite.close();
		}
		if(Helper._rewardMysql != null){
			Helper._rewardMysql.close();
		}
		if(Helper._rewardSQLLite != null){
			Helper._rewardSQLLite.close();
		}
		
		if(Helper._blocksBreakTaskId != null && Helper._blocksBreakTaskId != -1){
			Helper.StopAsyncTask(Helper._blocksBreakTaskId);
			Helper.CalcMoney(Helper._blocksBreaked);
		}
	}

	@Override
	public void onEnable() {
		Helper.setPlugin(this);
		Helper.LoadConfig();
		if(Helper.Config().getBoolean("config.Land.SellBySign")) {
			Helper.RegisterListener(new LandListener());
		}
		if(Helper.Config().getBoolean("config.Land.TaxEnabled")) {
			Helper.StartTax();
		}
		if(Helper.Config().getBoolean("config.ItemStacker.Enabled")) {
			Helper.RegisterListener(new ItemStackerListener());
		}
		if(Helper.Config().getBoolean("config.MoneyPerBlock.Enabled")) {
			Helper.RegisterListener(new MoneyPerBlockListener());
			Helper.StartMoneyPerBlockPay();
		}
		if(Helper.Config().getBoolean("config.AutoReplant.Enabled")) {
			Helper.RegisterListener(new AutoReplantListener());
		}
		
		if(Helper.Config().getBoolean("config.Land.Enabled")){
			this.getCommand("land").setExecutor(new LandCommandExecutor());
		}
		
		if(Helper.Config().getBoolean("config.Farmonator.Enabled")) {
			if(this.InitDB("Farmonator")) {
				Helper.LoadFarmonatorFromDB();
				this.getCommand("farm").setExecutor(new FarmCommandExecutor());
				Helper.RegisterListener(new FarmListener());
			}
		}
		if(Helper.Config().getBoolean("config.Reward.Enabled")) {
			this.InitDB("Reward");
			Helper.LoadRewardLocks();
			Helper.RegisterListener(new RewardListener());
		}
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (Exception ex) {
		    // Failed to submit the stats :-(
			Helper.LogError(ex.getMessage());
		}
		
		Helper.LogInfo("wurde erfolgreich geladen!");
	}

	private boolean InitDB(String TableName) {
		String host = Helper.Config().getString("config."+TableName+".db.Hostname");
		int port = Helper.Config().getInt("config."+TableName+".db.Port");
		String data = Helper.Config().getString("config."+TableName+".db.Database");
		String user = Helper.Config().getString("config."+TableName+".db.User");
		String pass = Helper.Config().getString("config."+TableName+".db.Password");
		return Helper.MySQLInit(host, port, data, user, pass, TableName);		
	}
}
