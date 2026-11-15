package com.igneko.aidirector.core.survivor;

/**
 * Estado vital de un superviviente. PINNED se añadirá en la Fase 5 como estado ortogonal
 * (un superviviente puede estar vivo y atrapado a la vez).
 */
public enum SurvivorState {
    ALIVE,
    INCAPACITATED,
    DEAD;

    public static SurvivorState byOrdinal(int ordinal) {
        SurvivorState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ALIVE;
    }
}
