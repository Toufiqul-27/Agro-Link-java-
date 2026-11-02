package com.agrolinkbd.agrolinkbd;

public class User {
    private int id;
    private String name, nid, phone, location, role, status, registeredOn;

    public User(int id, String name, String nid, String phone, String location, String role, String status, String registeredOn) {
        this.id = id;
        this.name = name;
        this.nid = nid;
        this.phone = phone;
        this.location = location;
        this.role = role;
        this.status = status;
        this.registeredOn = registeredOn;
    }


    public int getId() { return id; }
    public String getName() { return name; }
    public String getNid() { return nid; }
    public String getRegisteredOn() { return registeredOn; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
}