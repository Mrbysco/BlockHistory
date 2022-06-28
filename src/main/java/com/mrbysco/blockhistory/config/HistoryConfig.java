package com.mrbysco.blockhistory.config;

import com.mrbysco.blockhistory.BlockHistory;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

public class HistoryConfig {
	public static class Server {
		public final BooleanValue storeExplosions;
		public final BooleanValue storeContainerInteractions;
		public final BooleanValue storeContainerInventoryChanges;
		public final IntValue maxHistoryPerBlock;
		public final IntValue maxHistoryInChat;

		Server(ForgeConfigSpec.Builder builder) {
			builder.comment("Logging settings")
					.push("Logging");

			storeExplosions = builder
					.comment("Dictates if the mod stores explosion damage to the history log [Default: true]")
					.define("storeExplosions", true);

			storeContainerInteractions = builder
					.comment("Dictates if the mod stores interactions made with blocks that have a container (for example a chest) [Default: true]")
					.define("storeContainerInteractions", true);

			storeContainerInventoryChanges = builder
					.comment("Dictates if the mod stores inventory interactions made with blocks that have an inventory (for example a chest) [Default: true]")
					.define("storeContainerInventoryChanges", true);

			maxHistoryPerBlock = builder
					.comment("The max amount of history stored per block [Default: " + Integer.MAX_VALUE + "]")
					.defineInRange("maxHistoryPerBlock", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);

			maxHistoryInChat = builder
					.comment("The max amount of history stored per block [Default: 10]")
					.defineInRange("maxHistoryInChat", 10, 1, 200);

			builder.pop();
		}
	}

	public static final ForgeConfigSpec serverSpec;
	public static final Server SERVER;

	static {
		final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
		serverSpec = specPair.getRight();
		SERVER = specPair.getLeft();
	}

	@SubscribeEvent
	public static void onLoad(final ModConfigEvent.Loading configEvent) {
		BlockHistory.LOGGER.debug("Loaded Block History's config file {}", configEvent.getConfig().getFileName());
	}

	@SubscribeEvent
	public static void onFileChange(final ModConfigEvent.Reloading configEvent) {
		BlockHistory.LOGGER.warn("Block History's config just got changed on the file system!");
	}
}
