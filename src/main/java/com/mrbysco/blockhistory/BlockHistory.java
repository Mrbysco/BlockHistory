package com.mrbysco.blockhistory;

import com.mojang.logging.LogUtils;
import com.mrbysco.blockhistory.command.HistoryCommands;
import com.mrbysco.blockhistory.config.HistoryConfig;
import com.mrbysco.blockhistory.handler.BreakHandler;
import com.mrbysco.blockhistory.handler.InventoryHandler;
import com.mrbysco.blockhistory.handler.ModifyHandler;
import com.mrbysco.blockhistory.handler.PlaceHandler;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;
import org.tmatesoft.sqljet.core.SqlJetException;

import java.io.File;

@Mod(BlockHistory.MOD_ID)
public class BlockHistory {
	public static final String MOD_ID = "blockhistory";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final File personalFolder = new File(FMLPaths.MODSDIR.get().toFile(), "blockhistory");

	public BlockHistory() {
		try {
			UserHistoryDatabase.init();
		} catch (SqlJetException e) {
			LOGGER.error(e.getMessage());
		}

		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, HistoryConfig.serverSpec);
		FMLJavaModLoadingContext.get().getModEventBus().register(HistoryConfig.class);

		//Handlers
		MinecraftForge.EVENT_BUS.register(new BreakHandler());
		MinecraftForge.EVENT_BUS.register(new PlaceHandler());
		MinecraftForge.EVENT_BUS.register(new InventoryHandler());
		MinecraftForge.EVENT_BUS.register(new ModifyHandler());

		MinecraftForge.EVENT_BUS.addListener(this::onCommandEvent);

		//Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remoteVersionString, networkBool) -> true));
	}

	public void onCommandEvent(RegisterCommandsEvent event) {
		HistoryCommands.initializeCommands(event.getDispatcher());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onServerStart(final ServerStartedEvent event) {
		UserHistoryDatabase.removeHistory(HistoryConfig.SERVER.removeOlderThanDays.get());
	}
}
