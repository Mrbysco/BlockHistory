package com.mrbysco.blockhistory.storage;

import net.minecraft.resources.Identifier;

public class ChangeStorage {
	public final String date;
	public final String username;
	public final String change;
	public final Identifier resourceLocation;
	public final String extraData;

	public ChangeStorage(String date, String username, String change, Identifier resourceLocation) {
		this.date = date;
		this.username = username;
		this.change = change;
		this.resourceLocation = resourceLocation;
		this.extraData = "";
	}

	public ChangeStorage(String date, String username, String change, Identifier resourceLocation, String extraData) {
		this.date = date;
		this.username = username;
		this.change = change;
		this.resourceLocation = resourceLocation;
		this.extraData = extraData;
	}
}
