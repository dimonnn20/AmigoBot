package com.example.AmigoBot.service;

public enum Month {
    SEPTEMBER("Сентября");

    String name;

    Month(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
