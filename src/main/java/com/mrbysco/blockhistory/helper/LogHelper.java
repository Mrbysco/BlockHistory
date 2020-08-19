package com.mrbysco.blockhistory.helper;

import com.mrbysco.blockhistory.BlockHistory;
import com.mrbysco.blockhistory.storage.ChangeAction;
import com.mrbysco.blockhistory.storage.ChangeStorage;

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
                String changeTxt = getLogText(change);
                fileWriter.write(changeTxt + "\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getLogText(ChangeStorage change) {
        ChangeAction action = ChangeAction.getAction(change.change);
        return String.format("At %s %s has %s a block", change.date, change.username, action.getNicerName());
    }
}
