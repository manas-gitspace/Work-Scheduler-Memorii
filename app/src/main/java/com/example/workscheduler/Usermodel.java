
package com.example.workscheduler;

public class Usermodel{
    int id;
    String username;

    public Usermodel(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
}
