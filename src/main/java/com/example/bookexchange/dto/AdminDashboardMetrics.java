package com.example.bookexchange.dto;

public class AdminDashboardMetrics {
    private final long totalUsers;
    private final long totalListings;
    private final long totalRequests;
    private final long pendingRequests;

    public AdminDashboardMetrics(long totalUsers, long totalListings, long totalRequests, long pendingRequests) {
        this.totalUsers = totalUsers;
        this.totalListings = totalListings;
        this.totalRequests = totalRequests;
        this.pendingRequests = pendingRequests;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalListings() {
        return totalListings;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public long getPendingRequests() {
        return pendingRequests;
    }
}

