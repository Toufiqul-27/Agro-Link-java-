package com.agrolinkbd.agrolinkbd;

public class Order {
    private int orderId;
    private String cropName;
    private String buyerName;
    private String farmerName;
    private String status;
    private String orderDate;
    private int quantity;
    private double totalPrice;

    public Order(int orderId, String cropName, String buyerName, String farmerName, String status, String orderDate, int quantity, double totalPrice) {
        this.orderId = orderId;
        this.cropName = cropName;
        this.buyerName = buyerName;
        this.farmerName = farmerName;
        this.status = status;
        this.orderDate = orderDate;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }


    public int getOrderId() { return orderId; }
    public String getCropName() { return cropName; }
    public String getBuyerName() { return buyerName; }
    public String getFarmerName() { return farmerName; }
    public String getStatus() { return status; }
    public String getOrderDate() { return orderDate; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }


    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    public void setStatus(String status) { this.status = status; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}