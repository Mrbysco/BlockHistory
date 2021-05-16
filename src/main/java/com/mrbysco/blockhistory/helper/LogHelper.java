package com.mrbysco.blockhistory.helper;

import com.mrbysco.blockhistory.BlockHistory;
import com.mrbysco.blockhistory.storage.ChangeAction;
import com.mrbysco.blockhistory.storage.ChangeStorage;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class LogHelper {
    public static File logFile = new File(BlockHistory.personalFolder, "/log.txt");

    public static void logHistoryToFile(List<ChangeStorage> storageList) {
        try {
            FileWriter fileWriter = new FileWriter(logFile,false);
            for (ChangeStorage change : storageList) {
                String changeTxt = getLogText(change).getUnformattedComponentText();
                fileWriter.write(changeTxt + "\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ITextComponent getLogText(ChangeStorage change) {
        ChangeAction action = ChangeAction.getAction(change.change);
        if(action == ChangeAction.INVENTORY_INSERTION || action == ChangeAction.INVENTORY_WITHDRAWAL) {
            if(change.extraData != null && !change.extraData.isEmpty()) {
                TranslationTextComponent startComponent = new TranslationTextComponent("At %s %s has ", change.date, change.username);
                startComponent.mergeStyle(action.getColor());

                StringTextComponent changeListComponent = new StringTextComponent(change.extraData);
                changeListComponent.mergeStyle(TextFormatting.WHITE);
                TranslationTextComponent changeComponent = new TranslationTextComponent(action.getNicerName(), changeListComponent);
                changeComponent.mergeStyle(action.getColor());

                TranslationTextComponent endComponent = new TranslationTextComponent(" the inventory of block [%s]", change.resourceLocation.toString());
                endComponent.mergeStyle(action.getColor());

                ITextComponent logComponent = startComponent.appendSibling(changeComponent).appendSibling(endComponent);
                return logComponent;
            } else {
                TranslationTextComponent fallBackComponent = new TranslationTextComponent("At %s %s has %s the inventory of block [%s]", change.date, change.username, String.format(action.getNicerName(), "items"), change.resourceLocation.toString());
                fallBackComponent.mergeStyle(action.getColor());
                return fallBackComponent;
            }
        } else {
            TranslationTextComponent logComponent = new TranslationTextComponent("At %s %s has %s a block [%s]", change.date, change.username, action.getNicerName(), change.resourceLocation.toString());
            logComponent.mergeStyle(action.getColor());
            return logComponent;
        }
    }
}
