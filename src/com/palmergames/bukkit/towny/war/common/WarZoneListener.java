package com.palmergames.bukkit.towny.war.common;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplosionDamagesEntityEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;

public class WarZoneListener implements Listener {
	
	private final Towny plugin;
	
	public WarZoneListener(Towny instance) {

		plugin = instance;
	}
	
	@EventHandler
	public void onDestroy(TownyDestroyEvent event) {
		Player player = event.getPlayer();
		Material mat = event.getMaterial();
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow destroy for Event War if material is an EditableMaterial, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_edit_material", "destroy", mat.toString().toLowerCase()));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onBuild(TownyBuildEvent event) {
		Player player = event.getPlayer();
		Material mat = event.getMaterial();
		TownBlockStatus status = plugin.getCache(player).getStatus();
		
		// Allow build for Event War if material is an EditableMaterial, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War 
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_edit_material", "build", mat.toString().toLowerCase()));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onItemUse(TownyItemuseEvent event) {
		Player player = event.getPlayer();
		TownBlockStatus status = plugin.getCache(event.getPlayer()).getStatus();
		
		// Allow item_use for Event War if isAllowingItemUseInWarZone is true, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingItemUseInWarZone()) {				
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_use_item"));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onSwitchUse(TownySwitchEvent event) {
		Player player = event.getPlayer();
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow switch for Event War if isAllowingSwitchesInWarZone is true, FlagWar also handled here
		if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isAllowingSwitchesInWarZone()) {
				event.setCancelled(true);
				event.setMessage(Translation.of("msg_err_warzone_cannot_use_switches"));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onExplosionDamagingBlocks(TownyExplodingBlocksEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;

		List<Block> alreadyAllowed = event.getTownyFilteredBlockList();
		List<Block> toAllow = new ArrayList<Block>();
		
		int count = 0;
		for (Block block : event.getVanillaBlockList()) {
			// Wilderness, skip it.
			if (TownyAPI.getInstance().isWilderness(block))
				continue;
			
			// Non-warzone, skip it.
			if (!TownyUniverse.getInstance().hasWarEvent(TownyAPI.getInstance().getTownBlock(block.getLocation())))
				continue;
			
			// A war that doesn't allow any kind of explosions.
			if (!WarZoneConfig.isAllowingExplosionsInWarZone()) {
				// Remove from the alreadyAllowed list if it exists there.
				if (alreadyAllowed.contains(block))
					alreadyAllowed.remove(block);
				continue;
			}

			// A war that does allow explosions and explosions regenerate.
			if (WarZoneConfig.regenBlocksAfterExplosionInWarZone()) {
				// Skip this block if it is in the ignore list. TODO: with the blockdata nowadays this might not even be necessary.
				if (WarZoneConfig.getExplosionsIgnoreList().contains(block.getType().name()) || WarZoneConfig.getExplosionsIgnoreList().contains(block.getRelative(BlockFace.UP).getType().toString())) {
					// Remove from the alreadyAllowed list if it exists there.
					if (alreadyAllowed.contains(block))
						alreadyAllowed.remove(block);
					continue;
				}
				count++;
				TownyRegenAPI.beginProtectionRegenTask(block, count, TownyAPI.getInstance().getTownyWorld(block.getLocation().getWorld().getName()));
			}
			// This is an allowed explosion, so add it to our War-allowed list.
			toAllow.add(block);
		}
		// Add all TownyFilteredBlocks to our list, since our list will be used.
		toAllow.addAll(alreadyAllowed);
		
		// Return the list of allowed blocks for this block explosion event.
		event.setBlockList(toAllow);
	}

	@EventHandler
	public void onExplosionDamagingEntity(TownyExplosionDamagesEntityEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		/*
		 * Handle occasions in the wilderness first.
		 */
		if (TownyAPI.getInstance().isWilderness(event.getLocation()))
			return;

		/*
		 * Must be inside of a town.
		 */
		
		// Not in a war zone, do not modify the outcome of the event.
		if (!TownyUniverse.getInstance().hasWarEvent(TownyAPI.getInstance().getTownBlock(event.getLocation())))
			return;
			
		/*
		 * Stops any type of exploding damage if wars are not allowing explosions.
		 */
		if (!WarZoneConfig.isAllowingExplosionsInWarZone()) {
			event.setCancelled(true);
			return;
		}

		/*
		 * Explosions must be allowed, so un-cancel the event.
		 */
		event.setCancelled(false);
	}

	@EventHandler (priority=EventPriority.HIGH)
	public void onFlagWarFlagPlace(TownyBuildEvent event) {
		if (!(FlagWarConfig.isAllowingAttacks() && event.getMaterial() == FlagWarConfig.getFlagBaseMaterial()))
			return;
		Player player = event.getPlayer();
		Block block = player.getWorld().getBlockAt(event.getLocation());
		WorldCoord worldCoord = new WorldCoord(block.getWorld().getName(), Coord.parseCoord(block));
		
		if (plugin.getCache(player).getStatus() == TownBlockStatus.ENEMY) 
			try {
				if (FlagWar.callAttackCellEvent(plugin, player, block, worldCoord))
					event.setCancelled(false);
			} catch (TownyException e) {
				event.setMessage(e.getMessage());
			}
	}
}
