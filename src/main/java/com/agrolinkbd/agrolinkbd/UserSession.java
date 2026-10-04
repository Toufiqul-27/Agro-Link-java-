package com.agrolinkbd.agrolinkbd;

public class UserSession {
    public static int loggedInUserId;
    public static String loggedInUserName;
    public static String loggedInRole;

    public static void clearSession() {
        loggedInUserId = 0;
        loggedInUserName = null;
        loggedInRole = null;
    }
}