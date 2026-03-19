package com.example.bookexchange.service;

import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ExchangeRequest;
import com.example.bookexchange.entity.RequestStatus;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.repository.BookListingRepository;
import com.example.bookexchange.repository.ExchangeRequestRepository;
import com.example.bookexchange.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ExchangeRequestService handles all exchange request operations.
 * This service manages the lifecycle of exchange requests between buyers and sellers.
 *
 * Exchange Request Flow:
 * 1. Buyer creates an exchange request for a seller's book
 * 2. Seller can accept or reject the request
 * 3. Buyer can cancel a pending request
 */
@Service
public class ExchangeRequestService {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private BookListingRepository bookListingRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new exchange request from a buyer for a seller's book.
     * Buyers can only request a book from a seller, they cannot offer their own books.
     *
     * @param listingId The ID of the book listing the buyer wants
     * @param buyerId The ID of the buyer making the request
     * @param message Optional message from the buyer to the seller
     * @return The created ExchangeRequest
     * @throws IllegalArgumentException if listing not found or buyer not found
     */
    @Transactional
    public ExchangeRequest createExchangeRequest(Long listingId, Long buyerId, String message) {
        // Fetch the listing
        BookListing listing = bookListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Book listing not found"));

        // Fetch the buyer
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));

        // Create the exchange request
        ExchangeRequest request = new ExchangeRequest();
        request.setListing(listing);
        request.setBuyer(buyer);
        request.setMessage(message != null ? message.trim() : null);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        return exchangeRequestRepository.save(request);
    }

    /**
     * Overloaded method for backward compatibility (deprecated)
     */
    @Transactional
    public ExchangeRequest createExchangeRequest(Long listingId, Long buyerId, Long buyerBookId, String message) {
        // buyerBookId is ignored - feature removed
        return createExchangeRequest(listingId, buyerId, message);
    }

    /**
     * Retrieve an exchange request by ID.
     *
     * @param id The exchange request ID
     * @return Optional containing the ExchangeRequest if found
     */
    public Optional<ExchangeRequest> getExchangeRequestById(Long id) {
        return exchangeRequestRepository.findById(id);
    }

    /**
     * Get all exchange requests made by a specific buyer.
     *
     * @param buyer The buyer user
     * @return List of exchange requests made by the buyer
     */
    public List<ExchangeRequest> getRequestsByBuyer(User buyer) {
        return exchangeRequestRepository.findByBuyer(buyer);
    }

    /**
     * Get all exchange requests for books listed by a specific seller.
     * This returns all requests for the seller's book listings.
     *
     * @param seller The seller user
     * @return List of exchange requests for the seller's books
     */
    public List<ExchangeRequest> getRequestsForSeller(User seller) {
        // Get all listings for this seller
        List<BookListing> sellerListings = bookListingRepository.findBySeller(seller);

        // Get all exchange requests for these listings
        return sellerListings.stream()
                .flatMap(listing -> exchangeRequestRepository.findByListingId(listing.getId()).stream())
                .toList();
    }

    /**
     * Get all exchange requests for a specific book listing.
     *
     * @param listingId The book listing ID
     * @return List of exchange requests for the listing
     */
    public List<ExchangeRequest> getRequestsByListing(Long listingId) {
        return exchangeRequestRepository.findByListingId(listingId);
    }

    /**
     * Get all exchange requests filtered by status.
     *
     * @param status The request status to filter by
     * @return List of exchange requests with the specified status
     */
    public List<ExchangeRequest> getRequestsByStatus(RequestStatus status) {
        return exchangeRequestRepository.findByStatus(status);
    }

    /**
     * Accept an exchange request (seller accepts the buyer's request).
     * Updates the request status to ACCEPTED.
     *
     * @param requestId The exchange request ID
     * @param seller The seller user (must own the listing to accept)
     * @return The updated ExchangeRequest
     * @throws IllegalArgumentException if request not found, seller does not own the listing
     */
    @Transactional
    public ExchangeRequest acceptExchangeRequest(Long requestId, User seller) {
        ExchangeRequest request = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request not found"));

        // Verify that the seller owns the listing
        if (!request.getListing().getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("You do not have permission to accept this request");
        }

        // Only allow accepting pending requests
        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new IllegalArgumentException("Only pending requests can be accepted. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.ACCEPTED);
        return exchangeRequestRepository.save(request);
    }

    /**
     * Reject an exchange request (seller rejects the buyer's request).
     * Updates the request status to REJECTED.
     *
     * @param requestId The exchange request ID
     * @param seller The seller user (must own the listing to reject)
     * @return The updated ExchangeRequest
     * @throws IllegalArgumentException if request not found, seller does not own the listing
     */
    @Transactional
    public ExchangeRequest rejectExchangeRequest(Long requestId, User seller) {
        ExchangeRequest request = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request not found"));

        // Verify that the seller owns the listing
        if (!request.getListing().getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("You do not have permission to reject this request");
        }

        // Only allow rejecting pending requests
        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new IllegalArgumentException("Only pending requests can be rejected. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        return exchangeRequestRepository.save(request);
    }

    /**
     * Cancel an exchange request (buyer cancels their own request).
     * Updates the request status to CANCELLED.
     *
     * @param requestId The exchange request ID
     * @param buyer The buyer user (must be the one who made the request)
     * @return The updated ExchangeRequest
     * @throws IllegalArgumentException if request not found, buyer did not make the request
     */
    @Transactional
    public ExchangeRequest cancelExchangeRequest(Long requestId, User buyer) {
        ExchangeRequest request = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Exchange request not found"));

        // Verify that the buyer made this request
        if (!request.getBuyer().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        // Only allow canceling pending requests
        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new IllegalArgumentException("Only pending requests can be cancelled. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.CANCELLED);
        return exchangeRequestRepository.save(request);
    }

    /**
     * Delete an exchange request from the database.
     * Use with caution - typically requests should be marked as REJECTED/CANCELLED instead.
     *
     * @param requestId The exchange request ID
     */
    @Transactional
    public void deleteExchangeRequest(Long requestId) {
        exchangeRequestRepository.deleteById(requestId);
    }

    /**
     * Get count of pending requests for a buyer.
     *
     * @param buyer The buyer user
     * @return Number of pending requests made by the buyer
     */
    public long getPendingRequestCountForBuyer(User buyer) {
        return getRequestsByBuyer(buyer).stream()
                .filter(req -> req.getStatus().equals(RequestStatus.PENDING))
                .count();
    }

    /**
     * Get count of pending requests for a seller.
     *
     * @param seller The seller user
     * @return Number of pending requests for the seller's listings
     */
    public long getPendingRequestCountForSeller(User seller) {
        return getRequestsForSeller(seller).stream()
                .filter(req -> req.getStatus().equals(RequestStatus.PENDING))
                .count();
    }

    /**
     * Check if a buyer has already made a request for a specific listing.
     *
     * @param listingId The book listing ID
     * @param buyer The buyer user
     * @return True if a pending request exists, false otherwise
     */
    public boolean hasExistingPendingRequest(Long listingId, User buyer) {
        List<ExchangeRequest> requests = exchangeRequestRepository.findByListingId(listingId);
        return requests.stream()
                .anyMatch(req -> req.getBuyer().getId().equals(buyer.getId()) &&
                        req.getStatus().equals(RequestStatus.PENDING));
    }

    /**
     * Get all pending requests for a seller.
     *
     * @param seller The seller user
     * @return List of pending exchange requests for the seller's listings
     */
    public List<ExchangeRequest> getPendingRequestsForSeller(User seller) {
        return getRequestsForSeller(seller).stream()
                .filter(req -> req.getStatus().equals(RequestStatus.PENDING))
                .toList();
    }

    /**
     * Get all pending requests made by a buyer.
     *
     * @param buyer The buyer user
     * @return List of pending exchange requests made by the buyer
     */
    public List<ExchangeRequest> getPendingRequestsForBuyer(User buyer) {
        return getRequestsByBuyer(buyer).stream()
                .filter(req -> req.getStatus().equals(RequestStatus.PENDING))
                .toList();
    }
}
