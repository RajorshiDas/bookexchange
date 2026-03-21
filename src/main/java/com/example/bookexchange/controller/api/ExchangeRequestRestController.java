package com.example.bookexchange.controller.api;

import com.example.bookexchange.dto.api.ExchangeRequestCreateDto;
import com.example.bookexchange.dto.api.ExchangeRequestResponseDto;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ExchangeRequest;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.ForbiddenOperationException;
import com.example.bookexchange.exception.ResourceNotFoundException;
import com.example.bookexchange.repository.UserRepository;
import com.example.bookexchange.service.BookService;
import com.example.bookexchange.service.ExchangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class ExchangeRequestRestController {

    private final ExchangeRequestService exchangeRequestService;
    private final BookService bookService;
    private final UserRepository userRepository;

    public ExchangeRequestRestController(ExchangeRequestService exchangeRequestService,
                                         BookService bookService,
                                         UserRepository userRepository) {
        this.exchangeRequestService = exchangeRequestService;
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    @GetMapping("/buyer")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<ExchangeRequestResponseDto>> getBuyerRequests(Authentication authentication) {
        User buyer = getCurrentUser(authentication);
        List<ExchangeRequest> requests = exchangeRequestService.getRequestsByBuyer(buyer);
        return ResponseEntity.ok(requests.stream().map(this::toResponseDto).toList());
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ExchangeRequestResponseDto>> getSellerRequests(Authentication authentication) {
        User seller = getCurrentUser(authentication);
        List<ExchangeRequest> requests = exchangeRequestService.getRequestsForSeller(seller);
        return ResponseEntity.ok(requests.stream().map(this::toResponseDto).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    public ResponseEntity<ExchangeRequestResponseDto> getRequest(@PathVariable Long id,
                                                                 Authentication authentication) {
        ExchangeRequest request = exchangeRequestService.getExchangeRequestById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));
        User currentUser = getCurrentUser(authentication);
        Long buyerId = request.getBuyer().getId();
        Long sellerId = request.getListing().getSeller().getId();
        if (!buyerId.equals(currentUser.getId()) && !sellerId.equals(currentUser.getId())) {
            throw new ForbiddenOperationException("You do not have permission to view this request");
        }
        return ResponseEntity.ok(toResponseDto(request));
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ExchangeRequestResponseDto> createRequest(
            @Valid @RequestBody ExchangeRequestCreateDto request,
            Authentication authentication) {
        User buyer = getCurrentUser(authentication);
        if (!buyer.getId().equals(request.getBuyerId())) {
            throw new ForbiddenOperationException("You can only create requests for your own account");
        }

        BookListing listing = bookService.getBookListingById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Book listing not found"));

        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new BadRequestException("You cannot request your own listing");
        }

        if (exchangeRequestService.hasExistingPendingRequest(listing.getId(), buyer)) {
            throw new BadRequestException("You already have a pending request for this listing");
        }

        try {
            ExchangeRequest created = exchangeRequestService.createExchangeRequest(
                    listing.getId(),
                    buyer.getId(),
                    request.getMessage()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(created));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ExchangeRequestResponseDto> cancelRequest(@PathVariable Long id,
                                                                    Authentication authentication) {
        User buyer = getCurrentUser(authentication);
        try {
            ExchangeRequest updated = exchangeRequestService.cancelExchangeRequest(id, buyer);
            return ResponseEntity.ok(toResponseDto(updated));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ExchangeRequestResponseDto> acceptRequest(@PathVariable Long id,
                                                                    Authentication authentication) {
        User seller = getCurrentUser(authentication);
        try {
            ExchangeRequest updated = exchangeRequestService.acceptExchangeRequest(id, seller);
            return ResponseEntity.ok(toResponseDto(updated));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ExchangeRequestResponseDto> rejectRequest(@PathVariable Long id,
                                                                    Authentication authentication) {
        User seller = getCurrentUser(authentication);
        try {
            ExchangeRequest updated = exchangeRequestService.rejectExchangeRequest(id, seller);
            return ResponseEntity.ok(toResponseDto(updated));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ExchangeRequestResponseDto toResponseDto(ExchangeRequest request) {
        return ExchangeRequestResponseDto.builder()
                .id(request.getId())
                .listingId(request.getListing().getId())
                .bookTitle(request.getListing().getBook().getTitle())
                .buyerId(request.getBuyer().getId())
                .buyerUsername(request.getBuyer().getUsername())
                .buyerEmail(request.getBuyer().getEmail())
                .sellerId(request.getListing().getSeller().getId())
                .sellerUsername(request.getListing().getSeller().getUsername())
                .sellerEmail(request.getListing().getSeller().getEmail())
                .message(request.getMessage())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}

