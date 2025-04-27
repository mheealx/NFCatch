package com.esime.nfcdroid2.ui.informacion.componentes;

public class ChipGuess {

    public final String name;

    public ChipGuess(String name, float confidence) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
