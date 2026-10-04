package com.agrolinkbd.agrolinkbd;

public class Truck {
    private int id;
    private String route;
    private int capacity;
    private double costPerKg;
    private int bookedWeight;
    private int availableSpace;
    private String departureDate;
    private String status;

    public Truck(int id, String route, int capacity, double costPerKg, int bookedWeight, String departureDate, String status) {
        this.id = id;
        this.route = route;
        this.capacity = capacity;
        this.costPerKg = costPerKg;
        this.bookedWeight = bookedWeight;
        this.availableSpace = capacity - bookedWeight;
        this.departureDate = departureDate;
        this.status = status;
    }

    public int getId() { return id; }
    public String getRoute() { return route; }
    public int getCapacity() { return capacity; }
    public double getCostPerKg() { return costPerKg; }
    public int getBookedWeight() { return bookedWeight; }
    public int getAvailableSpace() { return availableSpace; }
    public String getDepartureDate() { return departureDate; }
    public String getStatus() { return status; }
}