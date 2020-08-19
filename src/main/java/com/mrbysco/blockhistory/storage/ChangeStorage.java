package com.mrbysco.blockhistory.storage;

public class ChangeStorage{
    public String date;
    public String username;
    public String change;

    public ChangeStorage(String date, String username, String change) {
        this.date = date;
        this.username = username;
        this.change = change;
    }
}
