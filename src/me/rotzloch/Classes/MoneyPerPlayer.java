package me.rotzloch.Classes;

import java.util.HashMap;
import java.util.Map;

public class MoneyPerPlayer implements Runnable{

	private Map<String, Integer> _blocksBreaked;
	
	public MoneyPerPlayer(){
	}
	
	@Override
	public void run() {
		this._blocksBreaked = new HashMap<String,Integer>();
		this._blocksBreaked.putAll(Helper._blocksBreaked);
		Helper._blocksBreaked.clear();
		Helper.CalcMoney(this._blocksBreaked);
	}

}
