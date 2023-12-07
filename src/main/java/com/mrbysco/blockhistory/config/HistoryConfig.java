package com.mrbysco.blockhistory.config;

import com.mrbysco.blockhistory.BlockHistory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class HistoryConfig {
	public static class Server {
		public final ModConfigSpec.BooleanValue storeExplosions;
		public final ModConfigSpec.BooleanValue storeContainerInteractions;
		public final ModConfigSpec.BooleanValue storeContainerInventoryChanges;
		public final ModConfigSpec.BooleanValue logToLog;
		public final IntValue maxHistoryPerBlock;
		public final IntValue maxHistoryInChat;
		public final ModConfigSpec.BooleanValue whitelistEnabled;
		public final ModConfigSpec.ConfigValue<List<? extends String>> whitelist;
		public final IntValue removeOlderThanDays;

		Server(ModConfigSpec.Builder builder) {
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

			logToLog = builder
					.comment("Dictates if the log command should log to `latest.log` [Default: false]")
					.define("logToLog", false);

			maxHistoryPerBlock = builder
					.comment("The max amount of history stored per block [Default: " + Integer.MAX_VALUE + "]")
					.defineInRange("maxHistoryPerBlock", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);

			maxHistoryInChat = builder
					.comment("The max amount of history stored per block [Default: 10]")
					.defineInRange("maxHistoryInChat", 10, 1, 200);

			whitelistEnabled = builder
					.comment("Dictates if the whitelist should be used [Default: false]")
					.define("whitelistEnabled", false);

			whitelist = builder
					.comment("The whitelist of dimensions to log in [Default: [\"minecraft:overworld\", \"minecraft:the_nether\", \"minecraft:the_end\"]]")
					.defineListAllowEmpty(List.of("whitelist"), () -> List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), o -> o instanceof String);

			removeOlderThanDays = builder
					.comment("The amount of days to keep history for (0 = Don't delete) [Default: 0]")
					.defineInRange("removeOlderThanDays", 0, 0, Integer.MAX_VALUE);

			builder.pop();
		}
	}

	public static final ModConfigSpec serverSpec;
	public static final Server SERVER;

	static {
		final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
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
