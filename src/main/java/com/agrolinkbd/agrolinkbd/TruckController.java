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

    private final ObservableList<Truck> truckList = FXCollections.observableArrayList();
    private final ObservableList<TruckBooking> myBookingsList = FXCollections.observableArrayList();

    @FXML public void initialize() {
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
        trucksTable.setItems(truckList);
        myBookingsTable.setItems(myBookingsList);
        loadAvailableTrucks();
        loadMyBookings();
        loadRoutes();
    }

    private void loadRoutes() {
        routeCombo.getItems().clear();
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT DISTINCT route FROM trucks WHERE status = 'Available'");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) routeCombo.getItems().add(rs.getString("route"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void handleCalculateCost(ActionEvent event) {
        Truck truck = trucksTable.getSelectionModel().getSelectedItem();
        if (truck == null) { errorLabel.setText("Select a truck."); return; }
        try {
            int weight = Integer.parseInt(weightField.getText().trim());
            if (weight <= 0 || weight > truck.getAvailableSpace()) {
                errorLabel.setText("Enter a positive weight within available capacity.");
                return;
            }
            costShareLabel.setText(String.format("%.2f BDT", weight * truck.getCostPerKg()));
            errorLabel.setText("");
        } catch (NumberFormatException e) {
            errorLabel.setText("Weight must be a positive whole number.");
        }
    }

    @FXML void handleBookTruck(ActionEvent event) {
        Truck selected = trucksTable.getSelectionModel().getSelectedItem();
        String pickup = pickupField.getText().trim();
        if (selected == null || pickup.isBlank() || UserSession.loggedInUserId <= 0) {
            errorLabel.setText("Choose a truck and provide a pickup location.");
            return;
        }
        int weight;
        try {
            weight = Integer.parseInt(weightField.getText().trim());
            if (weight <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            errorLabel.setText("Weight must be a positive whole number.");
            return;
        }

        try (Connection conn = Database.connect()) {
            try {
                conn.setAutoCommit(false);
                // Conditional update makes concurrent bookings capacity-safe.
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE trucks SET booked_weight_kg = booked_weight_kg + ? "
                      + "WHERE id = ? AND status = 'Available' "
                      + "AND booked_weight_kg + ? <= capacity_kg")) {
                    update.setInt(1, weight);
                    update.setInt(2, selected.getId());
                    update.setInt(3, weight);
                    if (update.executeUpdate() != 1) {
                        throw new SQLException("Truck is full or unavailable.");
                    }
                }
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO truck_bookings "
                      + "(user_id, truck_id, weight_kg, cost_share, pickup_location) "
                      + "VALUES (?, ?, ?, ?, ?)")) {
                    insert.setInt(1, UserSession.loggedInUserId);
                    insert.setInt(2, selected.getId());
                    insert.setInt(3, weight);
                    insert.setDouble(4, weight * selected.getCostPerKg());
                    insert.setString(5, pickup);
                    insert.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            errorLabel.setText("Booking successful!");
            costShareLabel.setText("");
            weightField.clear();
            loadAvailableTrucks();
            loadMyBookings();
        } catch (Exception e) {
            errorLabel.setText("Booking failed. Capacity may have changed.");
            e.printStackTrace();
            loadAvailableTrucks();
        }
    }

    private void loadAvailableTrucks() {
        truckList.clear();
        String sql = "SELECT * FROM trucks WHERE status = 'Available' "
                   + "AND booked_weight_kg < capacity_kg ORDER BY departure_date";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                truckList.add(new Truck(rs.getInt("id"), rs.getString("route"),
                        rs.getInt("capacity_kg"), rs.getDouble("cost_per_kg"),
                        rs.getInt("booked_weight_kg"), rs.getString("departure_date"),
                        rs.getString("status")));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadMyBookings() {
        myBookingsList.clear();
        String sql = "SELECT b.id, t.route, t.departure_date, t.status, b.weight_kg, b.cost_share "
                   + "FROM truck_bookings b JOIN trucks t ON b.truck_id = t.id WHERE b.user_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, UserSession.loggedInUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    myBookingsList.add(new TruckBooking(rs.getInt("id"), rs.getString("route"),
                            rs.getString("departure_date"), rs.getString("status"),
                            rs.getInt("weight_kg"), rs.getDouble("cost_share")));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void goBack(ActionEvent event) {
        AgroLinkApp.sceneChange("BuyerDashboard.fxml", "Buyer Dashboard", 900, 700);
    }
    @FXML void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}
