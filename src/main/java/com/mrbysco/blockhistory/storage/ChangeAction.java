package com.mrbysco.blockhistory.storage;

import net.minecraft.ChatFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum ChangeAction {
    ERROR("error", "error", ChatFormatting.RED),
    BREAK("break", "broken", ChatFormatting.RED),
    PLACE("place", "placed", ChatFormatting.GREEN),
    EXPLOSION("explosion", "exploded", ChatFormatting.RED),
    CONTAINER_OPEN("containeropen", "opened the container of", ChatFormatting.YELLOW),
    INVENTORY_WITHDRAWAL("inventory_withdrawal", "withdrew %s from", ChatFormatting.RED),
    INVENTORY_INSERTION("inventory_insertion", "inserted %s into", ChatFormatting.GREEN);

    private final String name;
    private final String nicerName;
    private final ChatFormatting color;
    ChangeAction(String friendlyName, String nicerName, ChatFormatting color) {
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

    public ChatFormatting getColor() {
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
