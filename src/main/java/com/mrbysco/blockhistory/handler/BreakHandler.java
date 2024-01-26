package com.mrbysco.blockhistory.handler;

import com.mrbysco.blockhistory.config.HistoryConfig;
import com.mrbysco.blockhistory.helper.MiscHelper;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class BreakHandler {

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBlockBreak(final BlockEvent.BreakEvent event) {
		if (!event.getLevel().isClientSide()) {
			Player player = event.getPlayer();
			Level level = player.level();
			if (MiscHelper.matchesWhitelist(level) && player != null && !(player instanceof FakePlayer)) {
				String username = player.getName().getString();
				ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), username, "break", ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()));
				UserHistoryDatabase.addHistory(event.getPos().asLong(), changeData);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onExplosionEvent(final ExplosionEvent.Detonate event) {
		if (!event.getLevel().isClientSide() && HistoryConfig.SERVER.storeExplosions.get()) {
			Entity entity = event.getExplosion().getDamageSource().getEntity();
			if (entity != null) {
				final Level level = event.getLevel();
				if (!MiscHelper.matchesWhitelist(level))
					return;
				if (entity instanceof Player player && !(entity instanceof FakePlayer)) {
					Map<Long, ChangeStorage> changeDataMap = new HashMap<>();
					for (BlockPos position : event.getAffectedBlocks()) {
						String username = player.getName().getString();
						BlockState state = level.getBlockState(position);
						ResourceLocation resourceLoc = ForgeRegistries.BLOCKS.getKey(state.getBlock());
						ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), username, "explosion", resourceLoc != null ? resourceLoc : new ResourceLocation("minecraft", "air"));
						changeDataMap.put(position.asLong(), changeData);
					}
					//Bulk the database insert to reduce the number of transactions
					UserHistoryDatabase.bulkAddHistory(changeDataMap);
				} else {
					if (ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()) != null) {
						Map<Long, ChangeStorage> changeDataMap = new HashMap<>();
						String mobName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
						for (BlockPos position : event.getAffectedBlocks()) {
							BlockState state = level.getBlockState(position);
							ResourceLocation resourceLoc = ForgeRegistries.BLOCKS.getKey(state.getBlock());
							ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), mobName, "explosion", resourceLoc != null ? resourceLoc : new ResourceLocation("minecraft", "air"));
							changeDataMap.put(position.asLong(), changeData);
						}
						//Bulk the database insert to reduce the number of transactions
						UserHistoryDatabase.bulkAddHistory(changeDataMap);
					}
				}
			}
		}
	}
}
