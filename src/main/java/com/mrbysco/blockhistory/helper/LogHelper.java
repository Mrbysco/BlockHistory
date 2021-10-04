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
                String changeTxt = getLogText(change).getContents();
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
                startComponent.withStyle(action.getColor());

                StringTextComponent changeListComponent = new StringTextComponent(change.extraData);
                changeListComponent.withStyle(TextFormatting.WHITE);
                TranslationTextComponent changeComponent = new TranslationTextComponent(action.getNicerName(), changeListComponent);
                changeComponent.withStyle(action.getColor());

                TranslationTextComponent endComponent = new TranslationTextComponent(" the inventory of block [%s]", change.resourceLocation.toString());
                endComponent.withStyle(action.getColor());

                ITextComponent logComponent = startComponent.append(changeComponent).append(endComponent);
                return logComponent;
            } else {
                TranslationTextComponent fallBackComponent = new TranslationTextComponent("At %s %s has %s the inventory of block [%s]", change.date, change.username, String.format(action.getNicerName(), "items"), change.resourceLocation.toString());
                fallBackComponent.withStyle(action.getColor());
                return fallBackComponent;
            }
        } else {
            TranslationTextComponent logComponent = new TranslationTextComponent("At %s %s has %s a block [%s]", change.date, change.username, action.getNicerName(), change.resourceLocation.toString());
            logComponent.withStyle(action.getColor());
            return logComponent;
        }
    }
}
