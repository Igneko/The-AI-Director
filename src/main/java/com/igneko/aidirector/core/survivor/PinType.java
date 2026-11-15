package com.igneko.aidirector.core.survivor;

/**
 * Tipo de agarre de un Infectado Especial. Determina la postura de la víctima y si todavía puede
 * disparar (solo durante el arrastre de la lengua, como en L4D2).
 */
public enum PinType {
    NONE(false, false),
    /** Derribado y desgarrado en el suelo (Acechador). */
    POUNCE(true, false),
    /** Arrastrado por la lengua: aún puede disparar al Estrangulador. */
    DRAG(false, true),
    /** Estrangulado, inmóvil. */
    CHOKE(false, false),
    /** Un Parásito le monta y dirige sus pasos. */
    RIDE(false, false),
    /** Llevado por delante en una embestida. */
    CARRY(false, false),
    /** Golpeado contra el suelo una y otra vez tras la embestida. */
    POUND(true, false);

    private final boolean lying;
    private final boolean canShoot;

    PinType(boolean lying, boolean canShoot) {
        this.lying = lying;
        this.canShoot = canShoot;
    }

    public boolean lying() { return lying; }
    public boolean canShoot() { return canShoot; }

    public static PinType byOrdinal(int ordinal) {
        PinType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
