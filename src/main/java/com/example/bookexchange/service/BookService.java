package com.example.bookexchange.service;

import com.example.bookexchange.entity.Book;
import com.example.bookexchange.entity.BookCondition;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ListingStatus;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.repository.BookListingRepository;
import com.example.bookexchange.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BookService handles all book and book listing CRUD operations.
 * Ensures that sellers can only manage their own book listings.
 */
@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookListingRepository bookListingRepository;

    /**
     * Get all book listings for a specific seller.
     *
     * @param seller The seller user
     * @return List of BookListings owned by the seller
     */
    public List<BookListing> getSellerBooks(User seller) {
        return bookListingRepository.findBySeller(seller);
    }

    /**
     * Get a specific book listing by ID.
     *
     * @param id The BookListing ID
     * @return Optional containing the BookListing if found
     */
    public Optional<BookListing> getBookListingById(Long id) {
        return bookListingRepository.findById(id);
    }

    /**
     * Verify that the logged-in seller owns the specified book listing.
     *
     * @param bookListingId The ID of the book listing
     * @param seller The seller user
     * @throws IllegalAccessError if the seller does not own this listing
     */
    public void verifySellerOwnership(Long bookListingId, User seller) {
        Optional<BookListing> listing = bookListingRepository.findById(bookListingId);
        if (listing.isEmpty()) {
            throw new IllegalArgumentException("Book listing not found");
        }
        if (!listing.get().getSeller().getId().equals(seller.getId())) {
            throw new IllegalAccessError("You do not have permission to modify this book listing");
        }
    }

    /**
     * Add a new book and create a listing for the seller.
     *
     * @param title Book title (required)
     * @param author Book author (required)
     * @param isbn ISBN (optional, unique)
     * @param category Book category (optional)
     * @param description Book description (optional)
     * @param condition Physical condition (required)
     * @param price Listing price (required)
     * @param seller The seller creating the listing
     * @return The created BookListing
     */
    @Transactional
    public BookListing addBook(String title, String author, String isbn,
                                String category, String description,
                                BookCondition condition, BigDecimal price,
                                User seller) {
        // Validate required fields
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title is required");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Book author is required");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Book condition is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid price is required");
        }

        // Create the Book entity
        Book book = new Book();
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setIsbn(isbn != null ? isbn.trim() : null);
        book.setCategory(category != null ? category.trim() : null);
        book.setDescription(description != null ? description.trim() : null);
        book.setCondition(condition);

        // Save the book first
        book = bookRepository.save(book);

        // Create the BookListing
        BookListing listing = new BookListing();
        listing.setBook(book);
        listing.setSeller(seller);
        listing.setPrice(price);
        listing.setStatus(ListingStatus.AVAILABLE);
        listing.setCreatedAt(LocalDateTime.now());

        // Save and return the listing
        return bookListingRepository.save(listing);
    }

    /**
     * Edit an existing book and its listing.
     * Only the seller who owns the listing can edit it.
     *
     * @param bookListingId The ID of the BookListing to edit
     * @param title Updated title
     * @param author Updated author
     * @param isbn Updated ISBN
     * @param category Updated category
     * @param description Updated description
     * @param condition Updated condition
     * @param price Updated price
     * @param seller The seller making the edit
     * @return The updated BookListing
     */
    @Transactional
    public BookListing editBook(Long bookListingId, String title, String author,
                                 String isbn, String category, String description,
                                 BookCondition condition, BigDecimal price,
                                 User seller) {
        // Verify ownership
        verifySellerOwnership(bookListingId, seller);

        // Validate required fields
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title is required");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Book author is required");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Book condition is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valid price is required");
        }

        // Fetch the listing
        BookListing listing = bookListingRepository.findById(bookListingId)
                .orElseThrow(() -> new IllegalArgumentException("Book listing not found"));

        // Update the associated Book
        Book book = listing.getBook();
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setIsbn(isbn != null ? isbn.trim() : null);
        book.setCategory(category != null ? category.trim() : null);
        book.setDescription(description != null ? description.trim() : null);
        book.setCondition(condition);

        // Save the updated book
        bookRepository.save(book);

        // Update the listing price (status remains unchanged unless explicitly modified)
        listing.setPrice(price);

        // Save and return the updated listing
        return bookListingRepository.save(listing);
    }

    /**
     * Delete a book listing and its associated book.
     * Only the seller who owns the listing can delete it.
     *
     * @param bookListingId The ID of the BookListing to delete
     * @param seller The seller making the deletion
     */
    @Transactional
    public void deleteBook(Long bookListingId, User seller) {
        // Verify ownership
        verifySellerOwnership(bookListingId, seller);

        // Fetch the listing to get the book ID
        BookListing listing = bookListingRepository.findById(bookListingId)
                .orElseThrow(() -> new IllegalArgumentException("Book listing not found"));

        Long bookId = listing.getBook().getId();

        // Delete the listing first (due to foreign key constraint)
        bookListingRepository.deleteById(bookListingId);

        // Delete the associated book
        bookRepository.deleteById(bookId);
    }

    /**
     * Get a book by its ID.
     *
     * @param id The Book ID
     * @return Optional containing the Book if found
     */
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
}
