package com.agrolinkbd.agrolinkbd;

public class Product {
    private int id;
    private String cropName;
    private String farmerName;
    private String location;
    private String harvestDate;
    private double price;
    private int stock;

    public Product(int id, String cropName, String farmerName, String location, String harvestDate, double price, int stock) {
        this.id = id;
        this.cropName = cropName;
        this.farmerName = farmerName;
        this.location = location;
        this.harvestDate = harvestDate;
        this.price = price;
        this.stock = stock;
    }


    public int getId() { return id; }
    public String getCropName() { return cropName; }
    public String getFarmerName() { return farmerName; }
    public String getLocation() { return location; }
    public String getHarvestDate() { return harvestDate; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
}