package com.mrbysco.blockhistory.handler;

import com.mrbysco.blockhistory.config.HistoryConfig;
import com.mrbysco.blockhistory.helper.InventoryHelper;
import com.mrbysco.blockhistory.helper.MiscHelper;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryHandler {
	private static final Map<UUID, Long> CONTAINER_PLACE_MAP = new HashMap<>();
	private static final Map<UUID, NonNullList<ItemStack>> CONTAINER_MAP = new HashMap<>();

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(final PlayerInteractEvent.RightClickBlock event) {
		if (!event.getLevel().isClientSide() && HistoryConfig.SERVER.storeContainerInteractions.get()) {
			final Player player = event.getEntity();
			if (player != null && !(player instanceof FakePlayer) && !player.isShiftKeyDown()) {
				final Level level = event.getLevel();
				final BlockPos position = event.getPos();
				BlockState state = level.getBlockState(position);
				if (MiscHelper.matchesWhitelist(level) && state.getMenuProvider(level, position) != null) {
					if (HistoryConfig.SERVER.storeContainerInventoryChanges.get()) {
						CONTAINER_PLACE_MAP.put(player.getUUID(), position.asLong());
					}
					String username = player.getName().getString();
					ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), username, "containeropen", ForgeRegistries.BLOCKS.getKey((state.getBlock())));
					UserHistoryDatabase.addHistory(position.asLong(), changeData);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerContainerOpen(final PlayerContainerEvent.Open event) {
		final Player player = event.getEntity();
		final Level level = player.level();
		if (!level.isClientSide() && MiscHelper.matchesWhitelist(level) && HistoryConfig.SERVER.storeContainerInventoryChanges.get()) {
			AbstractContainerMenu container = event.getContainer();
			if (container.getItems().size() >= 1) {
				CONTAINER_MAP.put(player.getUUID(), InventoryHelper.getContainerInventory(container));
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerContainerClose(final PlayerContainerEvent.Close event) {
		final Player player = event.getEntity();
		final Level level = player.level();
		if (!level.isClientSide() && MiscHelper.matchesWhitelist(level) && HistoryConfig.SERVER.storeContainerInventoryChanges.get()) {
			UUID playerUUID = player.getUUID();
			NonNullList<ItemStack> oldInventory = CONTAINER_MAP.getOrDefault(playerUUID, null);
			final AbstractContainerMenu container = event.getContainer();
			if (CONTAINER_PLACE_MAP.containsKey(playerUUID) && oldInventory != null && container != null) {
				NonNullList<ItemStack> currentInventory = InventoryHelper.getContainerInventory(container);
				int oldCount = InventoryHelper.getItemCount(oldInventory);
				int newCount = InventoryHelper.getItemCount(currentInventory);
				if (oldCount != newCount) {
					NonNullList<ItemStack> differenceList = InventoryHelper.getInventoryChange(oldInventory, currentInventory);
					String username = player.getName().getString();
					BlockPos position = BlockPos.of(CONTAINER_PLACE_MAP.get(playerUUID));
					ResourceLocation location = ForgeRegistries.BLOCKS.getKey(level.getBlockState(position).getBlock());
					ChangeStorage changeData = null;
					if (newCount < oldCount) {
						changeData = new ChangeStorage(MiscHelper.getDate(), username, "inventory_withdrawal", location, differenceList.toString());
//                        LOGGER.debug("User withdrew the following: {}",  differenceList);
					}
					if (newCount > oldCount) {
						changeData = new ChangeStorage(MiscHelper.getDate(), username, "inventory_insertion", location, differenceList.toString());
//                        LOGGER.debug("User inserted the following: {}", differenceList);
					}
					if (changeData != null) {
						UserHistoryDatabase.addHistory(position.asLong(), changeData);
					}
				}
			}
			CONTAINER_MAP.remove(playerUUID);
		}
	}
}
