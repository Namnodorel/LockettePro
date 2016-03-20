package me.crafter.mc.lockettepro;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Openable;

public class BlockPlayerListener implements Listener {

	// Quick protect for chests
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuickLockChest(PlayerInteractEvent event){
		if (!Config.isQuickProtectEnabled()) return;
		Action action = event.getAction();
		Player player = event.getPlayer();
		if (action == Action.RIGHT_CLICK_BLOCK && player.getItemInHand().getType() == Material.SIGN && !player.isSneaking()){
			if (!player.hasPermission("lockettepro.lock")) return;
			BlockFace blockface = event.getBlockFace();
			if (blockface == BlockFace.NORTH || blockface == BlockFace.WEST || blockface == BlockFace.EAST || blockface == BlockFace.SOUTH){
				Block block = event.getClickedBlock();
				if (block.getRelative(blockface).getType() != Material.AIR) return; // This location is obstructed
				if (LocketteProAPI.isLockable(block)){
					boolean locked = LocketteProAPI.isLocked(block);
					event.setCancelled(true);
					if (!locked && !LocketteProAPI.isUpDownLockedDoor(block)){
						Utils.removeASign(player);
						Utils.sendMessages(player, Config.getLang("locked-quick"));
						Utils.playLockEffect(player, block);
						Utils.putSignOn(block, blockface, Config.getDefaultPrivateString(), player.getName());
					} else if (!locked && LocketteProAPI.isOwnerUpDownLockedDoor(block, player.getName())){
						Utils.removeASign(player);
						Utils.sendMessages(player, Config.getLang("additional-sign-added-quick"));
						Utils.playLockEffect(player, block);
						Utils.putSignOn(block, blockface, Config.getDefaultAdditionalString(), "");
					} else if (LocketteProAPI.isOwner(block, player.getName())){
						Utils.removeASign(player);
						Utils.putSignOn(block, blockface, Config.getDefaultAdditionalString(), "");
						Utils.playLockEffect(player, block);
						Utils.sendMessages(player, Config.getLang("additional-sign-added-quick"));
					} else {
						Utils.playAccessDenyEffect(player, block);
						Utils.sendMessages(player, Config.getLang("cannot-lock-quick"));
					}
				}
			}
		}
	}
	
	// Manual protection
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onManualLock(SignChangeEvent event){
		if (event.getBlock().getType() != Material.WALL_SIGN) return;
		String topline = event.getLine(0);
		Player player = event.getPlayer();
		if (!player.hasPermission("lockettepro.lock")) return;
		if (LocketteProAPI.isLockString(topline) || LocketteProAPI.isAdditionalString(topline)){
			Block block = LocketteProAPI.getAttachedBlock(event.getBlock());
			if (LocketteProAPI.isLockable(block)){
				boolean locked = LocketteProAPI.isLocked(block);
				if (!locked && !LocketteProAPI.isUpDownLockedDoor(block)){
					if (LocketteProAPI.isLockString(topline)){
						Utils.sendMessages(player, Config.getLang("locked-manual"));
						Utils.playLockEffect(player, block);
					} else {
						Utils.sendMessages(player, Config.getLang("not-locked-yet-manual"));
						event.setLine(0, Config.getLang("sign-error"));
						Utils.playAccessDenyEffect(player, block);
					}
				} else if (!locked && LocketteProAPI.isOwnerUpDownLockedDoor(block, player.getName())){
					if (LocketteProAPI.isLockString(topline)){
						Utils.sendMessages(player, Config.getLang("cannot-lock-door-nearby-manual"));
						event.setLine(0, Config.getLang("sign-error"));
						Utils.playAccessDenyEffect(player, block);
					} else {
						Utils.sendMessages(player, Config.getLang("additional-sign-added-manual"));
						Utils.playLockEffect(player, block);
					}
				} else if (LocketteProAPI.isOwner(block, player.getName())){
					if (LocketteProAPI.isLockString(topline)){
						Utils.sendMessages(player, Config.getLang("block-already-locked-manual"));
						event.setLine(0, Config.getLang("sign-error"));
						Utils.playAccessDenyEffect(player, block);
					} else {
						Utils.sendMessages(player, Config.getLang("additional-sign-added-manual"));
						Utils.playLockEffect(player, block);
					}
				} else { // Not possible to fall here except override
					Utils.sendMessages(player, Config.getLang("block-already-locked-manual"));
					event.getBlock().breakNaturally();
					Utils.playAccessDenyEffect(player, block);
				}
			} else {
				Utils.sendMessages(player, Config.getLang("block-is-not-lockable"));
				event.setLine(0, Config.getLang("sign-error"));
				Utils.playAccessDenyEffect(player, block);
			}
		}
	}
	
	// Player select sign
	@EventHandler(priority = EventPriority.LOW)
	public void playerSelectSign(PlayerInteractEvent event){
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.WALL_SIGN){
			Block block = event.getClickedBlock();
			Player player = event.getPlayer();
			if (!player.hasPermission("lockettepro.edit")) return;
			if (LocketteProAPI.isOwnerOfSign(block, player.getName())){
				Utils.selectSign(player, block);
				Utils.sendMessages(player, Config.getLang("sign-selected"));
				Utils.playLockEffect(player, block);
			}
		}
	}
	
	// Player break sign
	@EventHandler(priority = EventPriority.MONITOR)
	public void onAttemptBreakSign(BlockBreakEvent event){
		if (event.isCancelled()) return;
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if (player.hasPermission("lockettepro.admin.break")) return;
		if (LocketteProAPI.isLockSign(block)){
			if (LocketteProAPI.isOwnerOfSign(block, player.getName())){
				Utils.sendMessages(player, Config.getLang("break-own-lock-sign"));
				// Remove additional signs?
			} else {
				Utils.sendMessages(player, Config.getLang("cannot-break-this-lock-sign"));
				event.setCancelled(true);
				Utils.playAccessDenyEffect(player, block);
			}
		} else if (LocketteProAPI.isAdditionalSign(block)){
			if (LocketteProAPI.isOwnerOfSign(block, player.getName())){
				Utils.sendMessages(player, Config.getLang("break-own-additional-sign"));
			} else if (!LocketteProAPI.isProtected(LocketteProAPI.getAttachedBlock(block))){
				Utils.sendMessages(player, Config.getLang("break-redundant-additional-sign"));
			} else {
				Utils.sendMessages(player, Config.getLang("cannot-break-this-additional-sign"));
				event.setCancelled(true);
				Utils.playAccessDenyEffect(player, block);
			}
		}
	}
	
	// Protect block from being destroyed
	@EventHandler(priority = EventPriority.MONITOR)
	public void onAttemptBreakLockedBlocks(BlockBreakEvent event){
		if (event.isCancelled()) return;
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if (LocketteProAPI.isLocked(block) || LocketteProAPI.isUpDownLockedDoor(block)){
			Utils.sendMessages(player, Config.getLang("block-is-locked"));
			event.setCancelled(true);
			Utils.playAccessDenyEffect(player, block);
		}
	}

	// Protect block from being used & handle double doors
	@EventHandler(priority = EventPriority.MONITOR)
	public void onAttemptInteractLockedBlocks(PlayerInteractEvent event){
		if (event.isCancelled()) return;
		Action action = event.getAction();
		switch (action){
		case LEFT_CLICK_BLOCK:
		case RIGHT_CLICK_BLOCK:
			Block block = event.getClickedBlock();
			Player player = event.getPlayer();
			if (!player.hasPermission("lockettepro.admin.use") && 
					((LocketteProAPI.isLocked(block) && !LocketteProAPI.isUser(block, player.getName())) || 
					(LocketteProAPI.isUpDownLockedDoor(block) && !LocketteProAPI.isUserUpDownLockedDoor(block, player.getName())))
					){
				Utils.sendMessages(player, Config.getLang("block-is-locked"));
				event.setCancelled(true);
				Utils.playAccessDenyEffect(player, block);
			} else { // Handle double doors
				if (action == Action.RIGHT_CLICK_BLOCK){
					if (LocketteProAPI.isDoubleDoorBlock(block) && LocketteProAPI.isLocked(block)){
						Block doorblock = LocketteProAPI.getBottomDoorBlock(block);
						BlockState doorstate = doorblock.getState();
						Openable openablestate = (Openable)doorstate.getData();
						boolean shouldopen = !openablestate.isOpen(); // Move to here
						int closetime = LocketteProAPI.getTimerSingleDoor(doorblock);
						List<Block> doors = new ArrayList<Block>();
						doors.add(doorblock);
						if (doorblock.getType() == Material.IRON_DOOR_BLOCK){
							LocketteProAPI.toggleDoor(doorblock, shouldopen);
						}
						for (BlockFace blockface : LocketteProAPI.newsfaces){
							Block relative = doorblock.getRelative(blockface);
							if (relative.getType() == doorblock.getType()){
								doors.add(relative);
								LocketteProAPI.toggleDoor(relative, shouldopen);
							}
						}
						if (closetime > 0){
							Bukkit.getScheduler().runTaskLater(LockettePro.getPlugin(), new DoorToggleTask(doors), closetime*20);
						}
					}
				}
			}
			break;
		default:
			break;
		}
	}
	
	// Protect block from interfere block
	@EventHandler(priority = EventPriority.MONITOR)
	public void onAttemptPlaceInterfereBlocks(BlockPlaceEvent event){
		if (event.isCancelled()) return;
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if (player.hasPermission("lockettepro.admin.interfere")) return;
		if (LocketteProAPI.mayInterfere(block, player.getName())){
			Utils.sendMessages(player, Config.getLang("cannot-interfere-with-others"));
			event.setCancelled(true);
			Utils.playAccessDenyEffect(player, block);		
		}
	}
	
	// Tell player about lockettepro
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlaceFirstBlockNotify(BlockPlaceEvent event){
		if (event.isCancelled()) return;
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if (!player.hasPermission("lockettepro.lock")) return;
		if (Utils.shouldNotify(player) && Config.isLockable(block.getType())){
			if (Config.isQuickProtectEnabled()){
				Utils.sendMessages(player, Config.getLang("you-can-quick-lock-it"));	
			} else {
				Utils.sendMessages(player, Config.getLang("you-can-manual-lock-it"));	
			}
		}
	}

}