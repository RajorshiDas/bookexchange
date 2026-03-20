package com.example.bookexchange.repository;

import com.example.bookexchange.entity.ExchangeRequest;
import com.example.bookexchange.entity.RequestStatus;
import com.example.bookexchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {

    // All requests made by a specific buyer
    List<ExchangeRequest> findByBuyer(User buyer);

    // All requests for a specific listing
    List<ExchangeRequest> findByListingId(Long listingId);

    // All requests filtered by status
    List<ExchangeRequest> findByStatus(RequestStatus status);

    // Count of requests filtered by status (for admin metrics)
    long countByStatus(RequestStatus status);
}
