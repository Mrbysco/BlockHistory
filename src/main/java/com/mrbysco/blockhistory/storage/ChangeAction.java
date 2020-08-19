package com.mrbysco.blockhistory.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum ChangeAction {
    ERROR("error", "error"),
    BREAK("break", "broken"),
    PLACE("place", "placed"),
    EXPLOSION("explosion", "exploded");

    private final String name;
    private final String nicerName;
    ChangeAction(String friendlyName, String nicerName) {
        this.name = friendlyName;
        this.nicerName = nicerName;
    }

    public String getName() {
        return name;
    }

    public String getNicerName() {
        return nicerName;
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
