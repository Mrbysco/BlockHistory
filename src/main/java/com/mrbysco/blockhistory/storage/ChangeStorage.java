package com.mrbysco.blockhistory.storage;

import net.minecraft.util.ResourceLocation;

public class ChangeStorage{
    public String date;
    public String username;
    public String change;
    public ResourceLocation resourceLocation;

    public ChangeStorage(String date, String username, String change, ResourceLocation resourceLocation) {
        this.date = date;
        this.username = username;
        this.change = change;
        this.resourceLocation = resourceLocation;
    }
}
