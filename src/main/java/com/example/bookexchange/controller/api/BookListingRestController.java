package com.example.bookexchange.controller.api;

import com.example.bookexchange.dto.api.BookListingRequestDto;
import com.example.bookexchange.dto.api.BookListingResponseDto;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ListingStatus;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.ResourceNotFoundException;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class BookListingRestController {

    private final BookService bookService;
    private final UserRepository userRepository;

    public BookListingRestController(BookService bookService, UserRepository userRepository) {
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    public ResponseEntity<List<BookListingResponseDto>> getAvailableListings() {
        List<BookListing> listings = bookService.getBookListingRepository()
                .findByStatus(ListingStatus.AVAILABLE);
        return ResponseEntity.ok(listings.stream().map(this::toResponseDto).toList());
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<BookListingResponseDto>> getSellerListings(Authentication authentication) {
        User seller = getCurrentUser(authentication);
        List<BookListing> listings = bookService.getSellerBooks(seller);
        return ResponseEntity.ok(listings.stream().map(this::toResponseDto).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    public ResponseEntity<BookListingResponseDto> getListing(@PathVariable Long id) {
        BookListing listing = bookService.getBookListingById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book listing not found"));
        return ResponseEntity.ok(toResponseDto(listing));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<BookListingResponseDto> createListing(
            @Valid @RequestBody BookListingRequestDto request,
            Authentication authentication) {
        User seller = getCurrentUser(authentication);
        try {
            BookListing listing = bookService.addBook(
                    request.getTitle(),
                    request.getAuthor(),
                    request.getIsbn(),
                    request.getCategory(),
                    request.getDescription(),
                    request.getCondition(),
                    request.getPrice(),
                    seller
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(listing));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<BookListingResponseDto> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody BookListingRequestDto request,
            Authentication authentication) {
        User seller = getCurrentUser(authentication);
        try {
            BookListing listing = bookService.editBook(
                    id,
                    request.getTitle(),
                    request.getAuthor(),
                    request.getIsbn(),
                    request.getCategory(),
                    request.getDescription(),
                    request.getCondition(),
                    request.getPrice(),
                    seller
            );
            return ResponseEntity.ok(toResponseDto(listing));
        } catch (IllegalAccessError ex) {
            throw new BadRequestException("You do not have permission to update this listing", ex);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id, Authentication authentication) {
        User seller = getCurrentUser(authentication);
        try {
            bookService.deleteBook(id, seller);
            return ResponseEntity.noContent().build();
        } catch (IllegalAccessError ex) {
            throw new BadRequestException("You do not have permission to delete this listing", ex);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private BookListingResponseDto toResponseDto(BookListing listing) {
        return BookListingResponseDto.builder()
                .id(listing.getId())
                .title(listing.getBook().getTitle())
                .author(listing.getBook().getAuthor())
                .isbn(listing.getBook().getIsbn())
                .category(listing.getBook().getCategory())
                .description(listing.getBook().getDescription())
                .condition(listing.getBook().getCondition())
                .price(listing.getPrice())
                .status(listing.getStatus())
                .createdAt(listing.getCreatedAt())
                .sellerId(listing.getSeller().getId())
                .sellerUsername(listing.getSeller().getUsername())
                .sellerEmail(listing.getSeller().getEmail())
                .build();
    }
}

