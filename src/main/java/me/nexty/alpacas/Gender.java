package me.nexty.alpacas;

public enum Gender {
    MALE('M'),
    FEMALE('F');

    private char abrv;

    Gender(char abrv){
        this.abrv = abrv;
    }

    public char getAbrv() {
        return abrv;
    }
}
