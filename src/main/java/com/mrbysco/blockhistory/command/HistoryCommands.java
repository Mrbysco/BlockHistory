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
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public class HistoryCommands {
    public static void initializeCommands (CommandDispatcher<CommandSource> dispatcher) {
        final LiteralArgumentBuilder<CommandSource> root = Commands.literal(BlockHistory.MOD_ID);
        root.requires((commandSource) -> commandSource.hasPermission(2))
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(HistoryCommands::showHistory))
                .then(Commands.literal("log").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(HistoryCommands::logHistory)));
        dispatcher.register(root);
    }

    private static int showHistory(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        final BlockPos position = BlockPosArgument.getOrLoadBlockPos(ctx, "pos");
        List<ChangeStorage> storageList = UserHistoryDatabase.getHistory(position.asLong());
        if(!storageList.isEmpty()) {
            ctx.getSource().sendSuccess(new StringTextComponent(String.format("History of X: %s, Y: %s, Z: %s",
                    (double) position.getX(), (double) position.getY(), (double) position.getZ())).withStyle(TextFormatting.DARK_GREEN), true);

            List<ChangeStorage> viewableList = new ArrayList<>(storageList);
            int maxInChat = HistoryConfig.SERVER.maxHistoryInChat.get();
            if(storageList.size() > maxInChat) {
                viewableList = storageList.subList(storageList.size()-maxInChat, storageList.size());
            }
            for(ChangeStorage change : viewableList) {
                ITextComponent feedback = LogHelper.getLogText(change);
                ctx.getSource().sendSuccess(feedback, true);
            }
        } else {
            ctx.getSource().sendSuccess(new StringTextComponent("No history is known for given location"), true);
        }

        return 0;
    }

    private static int logHistory(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        final BlockPos position = BlockPosArgument.getOrLoadBlockPos(ctx, "pos");
        List<ChangeStorage> storageList = UserHistoryDatabase.getHistory(position.asLong());
        if(!storageList.isEmpty()) {
            LogHelper.logHistoryToFile(storageList);
            ctx.getSource().sendSuccess(new StringTextComponent(String.format("History of X: %s, Y: %s, Z: %s has been saved to a log",
                    (double) position.getX(), (double) position.getY(), (double) position.getZ())).withStyle(TextFormatting.DARK_GREEN), true);
        } else {
            ctx.getSource().sendSuccess(new StringTextComponent("No history is known for given location"), true);
        }

        return 0;
    }
}
