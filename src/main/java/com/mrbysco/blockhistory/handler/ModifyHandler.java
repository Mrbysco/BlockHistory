package com.mrbysco.blockhistory.handler;

import com.mrbysco.blockhistory.helper.MiscHelper;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class ModifyHandler {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockEvent.FarmlandTrampleEvent event) {
		if (!event.getLevel().isClientSide()) {
			Entity entity = event.getEntity();
			final Level level = entity.level();
			if (!MiscHelper.matchesWhitelist(level))
				return;

			String name = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
			if (entity instanceof Player player) {
				name = player.getName().getString();
			}
			ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), name, "trample", ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()));
			UserHistoryDatabase.addHistory(event.getPos().asLong(), changeData);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockEvent.BlockToolModificationEvent event) {
		if (!event.getLevel().isClientSide()) {
			Player player = event.getPlayer();
			LevelAccessor level = event.getLevel();
			if (level instanceof ServerLevel serverLevel) {
				if (!MiscHelper.matchesWhitelist(serverLevel))
					return;
			}

			String name = player != null ? player.getName().getString() : "Unknown";
			if (event.getFinalState() != null) {
				String extraData = ForgeRegistries.ITEMS.getKey(event.getHeldItemStack().getItem()).toString();
				ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), name, "modify", ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()), extraData);
				UserHistoryDatabase.addHistory(event.getPos().asLong(), changeData);
			}
		}
	}
}
