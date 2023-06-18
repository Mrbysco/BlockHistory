package com.mrbysco.blockhistory.storage;

import net.minecraft.resources.ResourceLocation;

public class ChangeStorage {
	public String date;
	public String username;
	public String change;
	public ResourceLocation resourceLocation;
	public String extraData;

	public ChangeStorage(String date, String username, String change, ResourceLocation resourceLocation) {
		this.date = date;
		this.username = username;
		this.change = change;
		this.resourceLocation = resourceLocation;
		this.extraData = "";
	}

	public ChangeStorage(String date, String username, String change, ResourceLocation resourceLocation, String extraData) {
		this.date = date;
		this.username = username;
		this.change = change;
		this.resourceLocation = resourceLocation;
		this.extraData = extraData;
	}
}
