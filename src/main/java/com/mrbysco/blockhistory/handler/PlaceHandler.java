package com.mrbysco.blockhistory.handler;

import com.mrbysco.blockhistory.helper.MiscHelper;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class PlaceHandler {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
		if (!event.getLevel().isClientSide()) {
			Entity entity = event.getEntity();
			Level level = entity.level();
			if (MiscHelper.matchesWhitelist(level) && entity instanceof Player player && !(entity instanceof FakePlayer)) {
				String username = player.getName().getString();
				ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), username, "place", ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock()));
				UserHistoryDatabase.addHistory(event.getPos().asLong(), changeData);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onMultiBlockPlace(final BlockEvent.EntityMultiPlaceEvent event) {
		if (!event.getLevel().isClientSide()) {
			Entity entity = event.getEntity();
			Level level = entity.level();
			if (MiscHelper.matchesWhitelist(level) && entity instanceof Player player && !(entity instanceof FakePlayer)) {
				Map<Long, ChangeStorage> changeDataMap = new HashMap<>();
				for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
					String username = player.getName().getString();
					ChangeStorage changeData = new ChangeStorage(MiscHelper.getDate(), username, "place", ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock()));
					changeDataMap.put(snapshot.getPos().asLong(), changeData);
				}
				//Bulk the database insert to reduce the number of transactions
				UserHistoryDatabase.bulkAddHistory(changeDataMap);
			}
		}
	}
}
