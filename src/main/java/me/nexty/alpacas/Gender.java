package me.nexty.alpacas;

public enum Gender {
    MALE("M"),
    FEMALE("F");

    String name;

    Gender(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
