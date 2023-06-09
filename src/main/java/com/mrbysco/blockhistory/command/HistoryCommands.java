package com.mrbysco.blockhistory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrbysco.blockhistory.BlockHistory;
import com.mrbysco.blockhistory.config.HistoryConfig;
import com.mrbysco.blockhistory.helper.LogHelper;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import com.mrbysco.blockhistory.storage.UserHistoryDatabase;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class HistoryCommands {
	public static void initializeCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(BlockHistory.MOD_ID);
		root.requires((commandSource) -> commandSource.hasPermission(2))
				.then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(HistoryCommands::showHistory))
				.then(Commands.literal("log").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(HistoryCommands::logHistory)));
		dispatcher.register(root);
	}

	private static int showHistory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		final BlockPos position = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		List<ChangeStorage> storageList = UserHistoryDatabase.getHistory(position.asLong());
		if (!storageList.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal(String.format("History of X: %s, Y: %s, Z: %s",
					(double) position.getX(), (double) position.getY(), (double) position.getZ())).withStyle(ChatFormatting.DARK_GREEN), true);

			List<ChangeStorage> viewableList = new ArrayList<>(storageList);
			int maxInChat = HistoryConfig.SERVER.maxHistoryInChat.get();
			if (storageList.size() > maxInChat) {
				viewableList = storageList.subList(storageList.size() - maxInChat, storageList.size());
			}
			for (ChangeStorage change : viewableList) {
				ctx.getSource().sendSuccess(() -> LogHelper.getLogText(change), true);
			}
		} else {
			ctx.getSource().sendSuccess(() -> Component.literal("No history is known for given location"), true);
		}

		return 0;
	}

	private static int logHistory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		final BlockPos position = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
		List<ChangeStorage> storageList = UserHistoryDatabase.getHistory(position.asLong());
		if (!storageList.isEmpty()) {
			LogHelper.logHistoryToFile(storageList);
			ctx.getSource().sendSuccess(() -> Component.literal(String.format("History of X: %s, Y: %s, Z: %s has been saved to a log",
					(double) position.getX(), (double) position.getY(), (double) position.getZ())).withStyle(ChatFormatting.DARK_GREEN), true);
		} else {
			ctx.getSource().sendSuccess(() -> Component.literal("No history is known for given location"), true);
		}

		return 0;
	}
}
