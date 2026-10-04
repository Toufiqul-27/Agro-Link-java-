package com.agrolinkbd.agrolinkbd;

public class TruckBooking {
    private int bookingId;
    private String route;
    private String date;
    private String status;
    private int weight;
    private double cost;

    public TruckBooking(int bookingId, String route, String date, String status, int weight, double cost) {
        this.bookingId = bookingId;
        this.route = route;
        this.date = date;
        this.status = status;
        this.weight = weight;
        this.cost = cost;
    }


    public int getBookingId() { return bookingId; }
    public String getRoute() { return route; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public int getWeight() { return weight; }
    public double getCost() { return cost; }
}