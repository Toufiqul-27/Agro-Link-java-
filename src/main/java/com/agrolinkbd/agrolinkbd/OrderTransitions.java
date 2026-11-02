package com.agrolinkbd.agrolinkbd;

/** Centralizes allowed order state changes for the desktop application. */
public final class OrderTransitions {
    private OrderTransitions() {}

    public static boolean allowed(String role, boolean owner, String current, String next) {
        if ("Admin".equals(role)) {
            return ("Pending".equals(current) &&
                        ("Confirmed".equals(next) || "Cancelled".equals(next)))
                    || ("Confirmed".equals(current) &&
                        ("Dispatched".equals(next) || "Cancelled".equals(next)));
        }
        return ("Buyer".equals(role) || "Arotdar".equals(role))
                && owner && "Pending".equals(current) && "Cancelled".equals(next);
    }
}
