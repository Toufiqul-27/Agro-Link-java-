package com.agrolinkbd.agrolinkbd;

public class Product {
    private final int id;
    private final String cropName;
    private final String farmerName;
    private final String location;
    private final String harvestDate;
    private final double price;
    private final int stock;
    private String cropType;

    public Product(int id, String cropName, String farmerName, String location,
                   String harvestDate, double price, int stock) {
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
    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }
    public String getFarmerName() { return farmerName; }
    public String getLocation() { return location; }
    public String getHarvestDate() { return harvestDate; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
}
