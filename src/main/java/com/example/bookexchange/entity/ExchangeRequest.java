package com.example.bookexchange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a request from a buyer to exchange/acquire a book from a seller.
 */
@Entity
@Table(name = "exchange_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The book the buyer wants (seller's listing)
    @ManyToOne(optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private BookListing listing;

    // The buyer who is requesting the book
    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;


    // Optional message from the buyer to the seller
    @Column(length = 1000)
    private String message;

    // Status of the exchange request
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    // When the request was submitted
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = RequestStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
