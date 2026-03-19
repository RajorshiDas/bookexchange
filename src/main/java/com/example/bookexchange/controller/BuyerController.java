package com.example.bookexchange.controller;

import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ExchangeRequest;
import com.example.bookexchange.entity.ListingStatus;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.service.BookService;
import com.example.bookexchange.service.ExchangeRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * BuyerController handles all buyer-related operations.
 * Buyers can:
 * - View all available books from all sellers
 * - Create exchange requests for books they want
 * - View their own exchange requests and their statuses
 * - Cancel their pending exchange requests
 *
 * Only authenticated buyers can access these endpoints.
 */
@Controller
@RequestMapping("/buyer")
public class BuyerController {

    @Autowired
    private BookService bookService;

    @Autowired
    private ExchangeRequestService exchangeRequestService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get the currently logged-in buyer user from Spring Security.
     *
     * @param authentication Spring Security authentication object
     * @return The authenticated User object
     */
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * GET /buyer/books
     * Display all available books from all sellers.
     * Buyers can see all books available for exchange/purchase.
     */
    @GetMapping("/books")
    public String viewAllBooks(Model model, Authentication authentication) {
        try {
            User buyer = getCurrentUser(authentication);

            // Get all available book listings (from all sellers)
            List<BookListing> allListings = bookService.getBookListingRepository()
                    .findByStatus(ListingStatus.AVAILABLE);

            model.addAttribute("bookListings", allListings);
            model.addAttribute("buyer", buyer);

            return "buyer-books-list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * GET /buyer/books/{id}
     * Display details of a specific book listing.
     * Shows book information and allows buyer to make an exchange request.
     */
    @GetMapping("/books/{id}")
    public String viewBookDetails(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User buyer = getCurrentUser(authentication);

            // Get the book listing
            Optional<BookListing> listing = bookService.getBookListingById(id);
            if (listing.isEmpty()) {
                return "redirect:/buyer/books?error=Book not found";
            }

            // Check if buyer has already made a pending request for this book
            boolean hasPendingRequest = exchangeRequestService.hasExistingPendingRequest(id, buyer);

            model.addAttribute("bookListing", listing.get());
            model.addAttribute("hasPendingRequest", hasPendingRequest);
            model.addAttribute("buyer", buyer);

            return "buyer-book-details";
        } catch (Exception e) {
            return "redirect:/buyer/books?error=" + e.getMessage();
        }
    }

    /**
     * POST /buyer/books/{id}/request
     * Create an exchange request for a book.
     * The buyer requests to acquire the seller's book.
     * Note: Buyers can no longer offer their own books in exchange.
     */
    @PostMapping("/books/{id}/request")
    public String createExchangeRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String message,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            User buyer = getCurrentUser(authentication);

            // Check if book listing exists
            Optional<BookListing> listing = bookService.getBookListingById(id);
            if (listing.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Book not found");
                return "redirect:/buyer/books";
            }

            // Check if buyer is not the seller
            if (listing.get().getSeller().getId().equals(buyer.getId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot request your own books");
                return "redirect:/buyer/books";
            }

            // Check if buyer already has a pending request for this book
            if (exchangeRequestService.hasExistingPendingRequest(id, buyer)) {
                redirectAttributes.addFlashAttribute("error", "You already have a pending request for this book");
                return "redirect:/buyer/books/" + id;
            }

            // Create the exchange request
            exchangeRequestService.createExchangeRequest(id, buyer.getId(), message);

            redirectAttributes.addFlashAttribute("success",
                    "Exchange request sent successfully! The seller will review your request.");
            return "redirect:/buyer/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/buyer/books/" + id;
        }
    }

    /**
     * GET /buyer/requests
     * Display all exchange requests made by the buyer.
     * Shows status and details of each request.
     */
    @GetMapping("/requests")
    public String viewMyRequests(Model model, Authentication authentication) {
        try {
            User buyer = getCurrentUser(authentication);

            // Get all requests made by this buyer
            List<ExchangeRequest> requests = exchangeRequestService.getRequestsByBuyer(buyer);

            // Separate by status for easier display
            List<ExchangeRequest> pendingRequests = requests.stream()
                    .filter(req -> req.getStatus().name().equals("PENDING"))
                    .toList();
            List<ExchangeRequest> acceptedRequests = requests.stream()
                    .filter(req -> req.getStatus().name().equals("ACCEPTED"))
                    .toList();
            List<ExchangeRequest> rejectedRequests = requests.stream()
                    .filter(req -> req.getStatus().name().equals("REJECTED"))
                    .toList();
            List<ExchangeRequest> cancelledRequests = requests.stream()
                    .filter(req -> req.getStatus().name().equals("CANCELLED"))
                    .toList();

            model.addAttribute("allRequests", requests);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("acceptedRequests", acceptedRequests);
            model.addAttribute("rejectedRequests", rejectedRequests);
            model.addAttribute("cancelledRequests", cancelledRequests);
            model.addAttribute("buyer", buyer);

            return "buyer-requests-list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * GET /buyer/requests/{id}
     * Display details of a specific exchange request.
     */
    @GetMapping("/requests/{id}")
    public String viewRequestDetails(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User buyer = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                return "redirect:/buyer/requests?error=Request not found";
            }

            // Verify that the buyer owns this request
            if (!request.get().getBuyer().getId().equals(buyer.getId())) {
                return "redirect:/buyer/requests?error=Unauthorized access";
            }

            model.addAttribute("request", request.get());
            model.addAttribute("buyer", buyer);

            return "buyer-request-details";
        } catch (Exception e) {
            return "redirect:/buyer/requests?error=" + e.getMessage();
        }
    }

    /**
     * POST /buyer/requests/{id}/cancel
     * Cancel a pending exchange request made by the buyer.
     * Buyers can only cancel pending requests.
     */
    @PostMapping("/requests/{id}/cancel")
    public String cancelExchangeRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            User buyer = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Request not found");
                return "redirect:/buyer/requests";
            }

            // Verify that the buyer owns this request
            if (!request.get().getBuyer().getId().equals(buyer.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/buyer/requests";
            }

            // Check if request is pending
            if (!request.get().getStatus().name().equals("PENDING")) {
                redirectAttributes.addFlashAttribute("error",
                        "Cannot cancel a " + request.get().getStatus() + " request. Only pending requests can be cancelled.");
                return "redirect:/buyer/requests/" + id;
            }

            // Cancel the request
            exchangeRequestService.cancelExchangeRequest(id, buyer);

            redirectAttributes.addFlashAttribute("success", "Exchange request cancelled successfully.");
            return "redirect:/buyer/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/buyer/requests/" + id;
        }
    }

    /**
     * GET /buyer/requests/{id}/confirm-cancel
     * Display a confirmation page before cancelling a request.
     */
    @GetMapping("/requests/{id}/confirm-cancel")
    public String showCancelConfirmation(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User buyer = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                return "redirect:/buyer/requests?error=Request not found";
            }

            // Verify that the buyer owns this request
            if (!request.get().getBuyer().getId().equals(buyer.getId())) {
                return "redirect:/buyer/requests?error=Unauthorized access";
            }

            model.addAttribute("request", request.get());
            model.addAttribute("buyer", buyer);

            return "buyer-request-cancel-confirm";
        } catch (Exception e) {
            return "redirect:/buyer/requests?error=" + e.getMessage();
        }
    }

    /**
     * Helper method to expose BookService's repository for getting all listings by status.
     * This is a workaround for accessing repository methods through the service.
     * In a real application, consider adding a method to BookService.
     */
}
