package com.mrbysco.blockhistory.helper;

import com.mrbysco.blockhistory.config.HistoryConfig;
import net.minecraft.world.level.Level;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MiscHelper {
	public static String getDate() {
		Date date = Calendar.getInstance().getTime();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return dateFormat.format(date);
	}

	public static boolean matchesWhitelist(Level level) {
		if (HistoryConfig.SERVER.whitelistEnabled.get()) {
			return HistoryConfig.SERVER.whitelist.get().contains(level.dimension().location().toString());
		}
		return true;
	}
}
