package com.mrbysco.blockhistory.helper;

import com.mrbysco.blockhistory.BlockHistory;
import com.mrbysco.blockhistory.config.HistoryConfig;
import com.mrbysco.blockhistory.storage.ChangeAction;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class LogHelper {
	public static File logFile = new File(BlockHistory.personalFolder, "log.txt");

	public static void logHistoryToFile(List<ChangeStorage> storageList) {
		if (HistoryConfig.SERVER.logToLog.get()) {
			BlockHistory.LOGGER.info("Logging history:");
			BlockHistory.LOGGER.info("###############");
			for (ChangeStorage change : storageList) {
				String changeTxt = getLogText(change).getContents();
				BlockHistory.LOGGER.info(changeTxt);
			}
			BlockHistory.LOGGER.info("###############");
		} else {
			try {
				FileWriter fileWriter = new FileWriter(logFile, false);
				for (ChangeStorage change : storageList) {
					String changeTxt = getLogText(change).getContents();
					fileWriter.write(changeTxt + "\n");
				}
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static Component getLogText(ChangeStorage change) {
		ChangeAction action = ChangeAction.getAction(change.change);
		if (action == ChangeAction.INVENTORY_INSERTION || action == ChangeAction.INVENTORY_WITHDRAWAL) {
			if (change.extraData != null && !change.extraData.isEmpty()) {
				TranslatableComponent startComponent = new TranslatableComponent("At %s %s has ", change.date, change.username);
				startComponent.withStyle(action.getColor());

				TextComponent changeListComponent = new TextComponent(change.extraData);
				changeListComponent.withStyle(ChatFormatting.WHITE);
				TranslatableComponent changeComponent = new TranslatableComponent(action.getNicerName(), changeListComponent);
				changeComponent.withStyle(action.getColor());

				TranslatableComponent endComponent = new TranslatableComponent(" the inventory of block [%s]", change.resourceLocation.toString());
				endComponent.withStyle(action.getColor());

				return startComponent.append(changeComponent).append(endComponent);
			} else {
				TranslatableComponent fallBackComponent = new TranslatableComponent("At %s %s has %s the inventory of block [%s]", change.date, change.username, String.format(action.getNicerName(), "items"), change.resourceLocation.toString());
				fallBackComponent.withStyle(action.getColor());
				return fallBackComponent;
			}
		} else {
			TranslatableComponent logComponent = new TranslatableComponent("At %s %s has %s a block [%s]", change.date, change.username, action.getNicerName(), change.resourceLocation.toString());
			logComponent.withStyle(action.getColor());
			return logComponent;
		}
	}
}
