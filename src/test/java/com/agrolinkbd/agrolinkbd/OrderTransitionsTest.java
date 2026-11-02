package com.agrolinkbd.agrolinkbd;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderTransitionsTest {
    @Test void adminsCanProgressOrdersInOrder() {
        assertTrue(OrderTransitions.allowed("Admin", false, "Pending", "Confirmed"));
        assertTrue(OrderTransitions.allowed("Admin", false, "Confirmed", "Dispatched"));
        assertTrue(OrderTransitions.allowed("Admin", false, "Pending", "Cancelled"));
        assertFalse(OrderTransitions.allowed("Admin", false, "Dispatched", "Cancelled"));
        assertFalse(OrderTransitions.allowed("Admin", false, "Cancelled", "Confirmed"));
    }

    @Test void onlyBuyerOwnerCanCancelPendingOrder() {
        assertTrue(OrderTransitions.allowed("Buyer", true, "Pending", "Cancelled"));
        assertTrue(OrderTransitions.allowed("Arotdar", true, "Pending", "Cancelled"));
        assertFalse(OrderTransitions.allowed("Buyer", false, "Pending", "Cancelled"));
        assertFalse(OrderTransitions.allowed("Buyer", true, "Pending", "Dispatched"));
        assertFalse(OrderTransitions.allowed("Buyer", true, "Confirmed", "Cancelled"));
        assertFalse(OrderTransitions.allowed("Farmer", true, "Pending", "Cancelled"));
    }
}
