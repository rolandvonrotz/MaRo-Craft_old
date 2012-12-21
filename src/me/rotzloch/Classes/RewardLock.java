package me.rotzloch.Classes;

import org.bukkit.entity.Player;

public class RewardLock {
	private String _playerName;
	private long _timeLockMilliseconds;
	private String _rewardID;
	
	public RewardLock(Player player, long timeLockMilliseconds, String rewardID) {
		this._playerName = player.getName();
		this._timeLockMilliseconds = timeLockMilliseconds;
		this._rewardID = rewardID;
	}
	
	public RewardLock(String playername, String timeLock, String rewardID) {
		this._rewardID = rewardID;
		this._timeLockMilliseconds = Long.parseLong(timeLock);
		this._playerName = playername;
	}
	
	public String RewardPlayer() {
		return this._playerName;
	}
	
	public long RewardLockTill() {
		return this._timeLockMilliseconds;
	}
	
	public String RewardID() {
		return this._rewardID;
	}
}
