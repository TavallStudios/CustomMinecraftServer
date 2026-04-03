package com.customminecraftserver.javaedition;

public enum JavaNextState {
    STATUS(1),
    LOGIN(2);

    private final int id;

    JavaNextState(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static JavaNextState fromId(int id) {
        for (JavaNextState value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported Java next state: " + id);
    }
}
