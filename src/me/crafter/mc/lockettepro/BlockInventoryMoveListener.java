package me.crafter.mc.lockettepro;

import org.bukkit.block.BlockState;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BlockInventoryMoveListener implements Listener {
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onInventoryMove(InventoryMoveItemEvent event){
		if (Config.isItemTransferOutBlocked() || Config.getHopperMinecartAction() == (byte)1){
			if (isInventoryLocked(event.getSource())){
				event.setCancelled(true);
				// Additional Hopper Minecart Check
				if (event.getDestination().getHolder() instanceof HopperMinecart) {
					byte hopperminecartaction = Config.getHopperMinecartAction();
					switch (hopperminecartaction){
					// case 0 - Impossible
					// case 1 - Do not need additional action (already cancelled)
					case (byte)2: // Extra action - HopperMinecart removal
						((HopperMinecart)event.getDestination().getHolder()).remove();
						break;
					}
				}
				return;
			}
		}
		if (Config.isItemTransferInBlocked()){
			if (isInventoryLocked(event.getDestination())){
				event.setCancelled(true);
				return;
			}
		}
	}
	
	public boolean isInventoryLocked(Inventory inventory){
		InventoryHolder inventoryholder = inventory.getHolder();
		if (inventoryholder instanceof BlockState){
			if (LocketteProAPI.isLocked(((BlockState)inventoryholder).getBlock())){
				return true;
			}
		}
		return false;
	}
	
}