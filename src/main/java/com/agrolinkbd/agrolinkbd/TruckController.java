package com.agrolinkbd.agrolinkbd;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;

public class TruckController {

    @FXML private ComboBox<String> routeCombo;
    @FXML private TextField pickupField, weightField;
    @FXML private Label costShareLabel, errorLabel;
    @FXML private DatePicker bookingDatePicker;

    @FXML private TableView<Truck> trucksTable;
    @FXML private TableColumn<Truck, String> colRoute, colDate;
    @FXML private TableColumn<Truck, Integer> colCapacity, colBookedWeight, colAvailableSpace;
    @FXML private TableColumn<Truck, Double> colCostPerKg;

    @FXML private TableView<TruckBooking> myBookingsTable;
    @FXML private TableColumn<TruckBooking, Integer> colBookingId, colBookingWeight;
    @FXML private TableColumn<TruckBooking, String> colBookingRoute, colBookingDate, colBookingStatus;
    @FXML private TableColumn<TruckBooking, Double> colBookingCost;

    private ObservableList<Truck> truckList = FXCollections.observableArrayList();
    private ObservableList<TruckBooking> myBookingsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // TableView Bindings
        colRoute.setCellValueFactory(new PropertyValueFactory<>("route"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colCostPerKg.setCellValueFactory(new PropertyValueFactory<>("costPerKg"));
        colBookedWeight.setCellValueFactory(new PropertyValueFactory<>("bookedWeight"));
        colAvailableSpace.setCellValueFactory(new PropertyValueFactory<>("availableSpace"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("departureDate"));

        colBookingId.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        colBookingRoute.setCellValueFactory(new PropertyValueFactory<>("route"));
        colBookingDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colBookingStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colBookingWeight.setCellValueFactory(new PropertyValueFactory<>("weight"));
        colBookingCost.setCellValueFactory(new PropertyValueFactory<>("cost"));

        loadAvailableTrucks();
        loadMyBookings();
        loadRoutes();
    }

    private void loadRoutes() {
        routeCombo.getItems().clear();
        String sql = "SELECT DISTINCT route FROM trucks WHERE status = 'Available'";
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) { routeCombo.getItems().add(rs.getString("route")); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleCalculateCost(ActionEvent event) {
        Truck selected = trucksTable.getSelectionModel().getSelectedItem();
        if (selected == null || weightField.getText().isEmpty()) {
            errorLabel.setText("Select a truck and enter weight!");
            return;
        }
        try {
            int weight = Integer.parseInt(weightField.getText());
            if (weight > selected.getAvailableSpace()) {
                errorLabel.setText("Not enough space!");
                return;
            }
            costShareLabel.setText(String.format("%.2f BDT", weight * selected.getCostPerKg()));
            errorLabel.setText("");
        } catch (NumberFormatException e) { errorLabel.setText("Invalid weight!"); }
    }

    @FXML
    void handleBookTruck(ActionEvent event) {
        Truck selected = trucksTable.getSelectionModel().getSelectedItem();
        if (selected == null || weightField.getText().isEmpty() || pickupField.getText().isEmpty()) {
            errorLabel.setText("Fill all fields (Truck, Weight, Pickup)!");
            return;
        }

        int weight = Integer.parseInt(weightField.getText());
        String sqlUpdate = "UPDATE trucks SET booked_weight_kg = booked_weight_kg + ? WHERE id = ?";
        // pickup_location যোগ করা হয়েছে
        String sqlInsert = "INSERT INTO truck_bookings (user_id, truck_id, weight_kg, cost_share, pickup_location) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(sqlUpdate);
                 PreparedStatement pstmt2 = conn.prepareStatement(sqlInsert)) {

                pstmt1.setInt(1, weight);
                pstmt1.setInt(2, selected.getId());
                pstmt1.executeUpdate();

                pstmt2.setInt(1, UserSession.loggedInUserId);
                pstmt2.setInt(2, selected.getId());
                pstmt2.setInt(3, weight);
                pstmt2.setDouble(4, weight * selected.getCostPerKg());
                pstmt2.setString(5, pickupField.getText()); // Pickup location
                pstmt2.executeUpdate();

                conn.commit();
                errorLabel.setText("Booking Successful!");
                loadAvailableTrucks();
                loadMyBookings();
            }
        } catch (SQLException e) { e.printStackTrace(); errorLabel.setText("Booking Failed!"); }
    }

    private void loadAvailableTrucks() {
        truckList.clear();
        String sql = "SELECT * FROM trucks WHERE status = 'Available'";
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                truckList.add(new Truck(rs.getInt("id"), rs.getString("route"), rs.getInt("capacity_kg"), rs.getDouble("cost_per_kg"), rs.getInt("booked_weight_kg"), rs.getString("departure_date"), rs.getString("status")));
            }
            trucksTable.setItems(truckList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadMyBookings() {
        myBookingsList.clear();
        String sql = "SELECT b.id, t.route, t.departure_date, t.status, b.weight_kg, b.cost_share FROM truck_bookings b JOIN trucks t ON b.truck_id = t.id WHERE b.user_id = " + UserSession.loggedInUserId;
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                myBookingsList.add(new TruckBooking(rs.getInt("id"), rs.getString("route"), rs.getString("departure_date"), rs.getString("status"), rs.getInt("weight_kg"), rs.getDouble("cost_share")));
            }
            myBookingsTable.setItems(myBookingsList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML void goBack(ActionEvent event) { AgroLinkApp.sceneChange("BuyerDashboard.fxml", "Buyer Dashboard", 900, 600); }
    @FXML void handleLogout(ActionEvent event) { UserSession.clearSession(); AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500); }
}