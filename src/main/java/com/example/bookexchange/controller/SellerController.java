package com.example.bookexchange.controller;

import com.example.bookexchange.entity.Book;
import com.example.bookexchange.entity.BookCondition;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.service.BookService;
import com.example.bookexchange.service.ExchangeRequestService;
import com.example.bookexchange.entity.ExchangeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * SellerController handles all seller-related book management operations.
 * Only authenticated sellers can access these endpoints.
 * Sellers can only manage their own books.
 */
@Controller
@RequestMapping("/seller")
public class SellerController {

    @Autowired
    private BookService bookService;

    @Autowired
    private ExchangeRequestService exchangeRequestService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get the currently logged-in seller user from Spring Security.
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
     * GET /seller/books
     * Display all books listed by the currently logged-in seller.
     */
    @GetMapping("/books")
    public String listBooks(Model model, Authentication authentication) {
        User seller = getCurrentUser(authentication);
        var bookListings = bookService.getSellerBooks(seller);
        model.addAttribute("bookListings", bookListings);
        return "books-list";
    }

    /**
     * GET /seller/books/add
     * Display the form to add a new book.
     */
    @GetMapping("/books/add")
    public String showAddBookForm(Model model) {
        // Provide all available book conditions for the dropdown
        model.addAttribute("conditions", BookCondition.values());
        return "book-add";
    }

    /**
     * POST /seller/books/add
     * Process the form submission to create a new book and listing.
     */
    @PostMapping("/books/add")
    public String addBook(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description,
            @RequestParam BookCondition condition,
            @RequestParam BigDecimal price,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);
            bookService.addBook(title, author, isbn, category, description, condition, price, seller);

            redirectAttributes.addFlashAttribute("success",
                    "Book added successfully! Your listing is now available.");
            return "redirect:/seller/books";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("conditions", BookCondition.values());
            model.addAttribute("title", title);
            model.addAttribute("author", author);
            model.addAttribute("isbn", isbn);
            model.addAttribute("category", category);
            model.addAttribute("description", description);
            model.addAttribute("price", price);
            return "book-add";
        }
    }

    /**
     * GET /seller/books/edit/{id}
     * Display the form to edit an existing book listing.
     */
    @GetMapping("/books/edit/{id}")
    public String showEditBookForm(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Verify ownership
            bookService.verifySellerOwnership(id, seller);

            // Fetch the listing and populate the form
            Optional<BookListing> listing = bookService.getBookListingById(id);
            if (listing.isEmpty()) {
                return "redirect:/seller/books?error=Book not found";
            }

            model.addAttribute("bookListing", listing.get());
            model.addAttribute("book", listing.get().getBook());
            model.addAttribute("conditions", BookCondition.values());
            return "book-edit";
        } catch (IllegalAccessError e) {
            return "redirect:/seller/books?error=Unauthorized access";
        } catch (Exception e) {
            return "redirect:/seller/books?error=" + e.getMessage();
        }
    }

    /**
     * POST /seller/books/edit/{id}
     * Process the form submission to update an existing book listing.
     */
    @PostMapping("/books/edit/{id}")
    public String editBook(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description,
            @RequestParam BookCondition condition,
            @RequestParam BigDecimal price,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Edit the book
            bookService.editBook(id, title, author, isbn, category, description, condition, price, seller);

            redirectAttributes.addFlashAttribute("success", "Book updated successfully!");
            return "redirect:/seller/books";
        } catch (IllegalAccessError e) {
            return "redirect:/seller/books?error=Unauthorized access";
        } catch (Exception e) {
            // Re-populate the form with data and error message
            model.addAttribute("error", e.getMessage());
            model.addAttribute("conditions", BookCondition.values());

            // Try to fetch the listing again for the form
            Optional<BookListing> listing = bookService.getBookListingById(id);
            if (listing.isPresent()) {
                model.addAttribute("bookListing", listing.get());
                model.addAttribute("book", listing.get().getBook());
            }

            return "book-edit";
        }
    }

    /**
     * GET /seller/books/delete/{id}
     * Display a confirmation page before deletion.
     */
    @GetMapping("/books/delete/{id}")
    public String showDeleteConfirmation(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Verify ownership
            bookService.verifySellerOwnership(id, seller);

            // Fetch the listing to display details
            Optional<BookListing> listing = bookService.getBookListingById(id);
            if (listing.isEmpty()) {
                return "redirect:/seller/books?error=Book not found";
            }

            model.addAttribute("bookListing", listing.get());
            return "book-delete-confirm";
        } catch (IllegalAccessError e) {
            return "redirect:/seller/books?error=Unauthorized access";
        } catch (Exception e) {
            return "redirect:/seller/books?error=" + e.getMessage();
        }
    }

    /**
     * POST /seller/books/delete/{id}
     * Process the deletion request.
     */
    @PostMapping("/books/delete/{id}")
    public String deleteBook(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Delete the book listing
            bookService.deleteBook(id, seller);

            redirectAttributes.addFlashAttribute("success", "Book deleted successfully!");
            return "redirect:/seller/books";
        } catch (IllegalAccessError e) {
            return "redirect:/seller/books?error=Unauthorized access";
        } catch (Exception e) {
            return "redirect:/seller/books?error=" + e.getMessage();
        }
    }

    /**
     * GET /seller/requests
     * Display all exchange requests for the seller's books.
     * Sellers can see all pending, accepted, and rejected requests.
     */
    @GetMapping("/requests")
    public String viewRequests(Model model, Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Get all requests for this seller's books
            List<ExchangeRequest> allRequests = exchangeRequestService.getRequestsForSeller(seller);

            // Separate by status for easier display
            List<ExchangeRequest> pendingRequests = allRequests.stream()
                    .filter(req -> req.getStatus().name().equals("PENDING"))
                    .toList();
            List<ExchangeRequest> acceptedRequests = allRequests.stream()
                    .filter(req -> req.getStatus().name().equals("ACCEPTED"))
                    .toList();
            List<ExchangeRequest> rejectedRequests = allRequests.stream()
                    .filter(req -> req.getStatus().name().equals("REJECTED"))
                    .toList();

            model.addAttribute("allRequests", allRequests);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("acceptedRequests", acceptedRequests);
            model.addAttribute("rejectedRequests", rejectedRequests);
            model.addAttribute("seller", seller);

            return "seller-requests-list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/seller/books";
        }
    }

    /**
     * GET /seller/requests/{id}
     * Display details of a specific exchange request.
     * Shows book details, buyer information, and request message.
     */
    @GetMapping("/requests/{id}")
    public String viewRequestDetails(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                return "redirect:/seller/requests?error=Request not found";
            }

            // Verify that the seller owns this listing
            if (!request.get().getListing().getSeller().getId().equals(seller.getId())) {
                return "redirect:/seller/requests?error=Unauthorized access";
            }

            model.addAttribute("request", request.get());
            model.addAttribute("seller", seller);

            return "seller-request-details";
        } catch (Exception e) {
            return "redirect:/seller/requests?error=" + e.getMessage();
        }
    }

    /**
     * POST /seller/requests/{id}/accept
     * Accept an exchange request from a buyer.
     * Updates the request status to ACCEPTED.
     */
    @PostMapping("/requests/{id}/accept")
    public String acceptRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Request not found");
                return "redirect:/seller/requests";
            }

            // Verify that the seller owns this listing
            if (!request.get().getListing().getSeller().getId().equals(seller.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/seller/requests";
            }

            // Accept the request
            exchangeRequestService.acceptExchangeRequest(id, seller);

            redirectAttributes.addFlashAttribute("success",
                    "Exchange request accepted! You can now proceed with the exchange.");
            return "redirect:/seller/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/requests/" + id;
        }
    }

    /**
     * POST /seller/requests/{id}/reject
     * Reject an exchange request from a buyer.
     * Updates the request status to REJECTED.
     */
    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Request not found");
                return "redirect:/seller/requests";
            }

            // Verify that the seller owns this listing
            if (!request.get().getListing().getSeller().getId().equals(seller.getId())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/seller/requests";
            }

            // Reject the request
            exchangeRequestService.rejectExchangeRequest(id, seller);

            redirectAttributes.addFlashAttribute("success", "Exchange request rejected.");
            return "redirect:/seller/requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/requests/" + id;
        }
    }

    /**
     * GET /seller/requests/{id}/confirm-accept
     * Display confirmation page before accepting a request.
     */
    @GetMapping("/requests/{id}/confirm-accept")
    public String showAcceptConfirmation(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                return "redirect:/seller/requests?error=Request not found";
            }

            // Verify that the seller owns this listing
            if (!request.get().getListing().getSeller().getId().equals(seller.getId())) {
                return "redirect:/seller/requests?error=Unauthorized access";
            }

            model.addAttribute("request", request.get());
            model.addAttribute("seller", seller);

            return "seller-request-accept-confirm";
        } catch (Exception e) {
            return "redirect:/seller/requests?error=" + e.getMessage();
        }
    }

    /**
     * GET /seller/requests/{id}/confirm-reject
     * Display confirmation page before rejecting a request.
     */
    @GetMapping("/requests/{id}/confirm-reject")
    public String showRejectConfirmation(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            User seller = getCurrentUser(authentication);

            // Get the exchange request
            Optional<ExchangeRequest> request = exchangeRequestService.getExchangeRequestById(id);
            if (request.isEmpty()) {
                return "redirect:/seller/requests?error=Request not found";
            }

            // Verify that the seller owns this listing
            if (!request.get().getListing().getSeller().getId().equals(seller.getId())) {
                return "redirect:/seller/requests?error=Unauthorized access";
            }

            model.addAttribute("request", request.get());
            model.addAttribute("seller", seller);

            return "seller-request-reject-confirm";
        } catch (Exception e) {
            return "redirect:/seller/requests?error=" + e.getMessage();
        }
    }
}
