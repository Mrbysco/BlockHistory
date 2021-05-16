package com.mrbysco.blockhistory.storage;

import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum ChangeAction {
    ERROR("error", "error", TextFormatting.RED),
    BREAK("break", "broken", TextFormatting.RED),
    PLACE("place", "placed", TextFormatting.GREEN),
    EXPLOSION("explosion", "exploded", TextFormatting.RED),
    CONTAINER_OPEN("containeropen", "opened the container of", TextFormatting.YELLOW),
    INVENTORY_WITHDRAWAL("inventory_withdrawal", "withdrew %s from", TextFormatting.RED),
    INVENTORY_INSERTION("inventory_insertion", "inserted %s into", TextFormatting.GREEN);

    private final String name;
    private final String nicerName;
    private final TextFormatting color;
    ChangeAction(String friendlyName, String nicerName, TextFormatting color) {
        this.name = friendlyName;
        this.nicerName = nicerName;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getNicerName() {
        return nicerName;
    }

    public TextFormatting getColor() {
        return color;
    }

    @Nonnull
    public static ChangeAction getAction(@Nullable String name) {
        for(ChangeAction event : ChangeAction.values()) {
            if(event.getName().equalsIgnoreCase(name)) {
                return event;
            }
        }

        return ERROR;
    }
}
