package com.example.bookexchange.service;

import com.example.bookexchange.entity.Book;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ExchangeRequest;
import com.example.bookexchange.entity.RequestStatus;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.ForbiddenOperationException;
import com.example.bookexchange.exception.ResourceNotFoundException;
import com.example.bookexchange.repository.BookListingRepository;
import com.example.bookexchange.repository.ExchangeRequestRepository;
import com.example.bookexchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRequestServiceTest {

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;

    @Mock
    private BookListingRepository bookListingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExchangeRequestService exchangeRequestService;

    @Captor
    private ArgumentCaptor<ExchangeRequest> requestCaptor;

    @Test
    void getRequestsByBuyer_returnsRepositoryResults() {
        User buyer = userWithId(10L);
        List<ExchangeRequest> expected = List.of(new ExchangeRequest());

        when(exchangeRequestRepository.findByBuyer(buyer)).thenReturn(expected);

        List<ExchangeRequest> result = exchangeRequestService.getRequestsByBuyer(buyer);

        assertSame(expected, result);
        verify(exchangeRequestRepository).findByBuyer(buyer);
    }

    @Test
    void getExchangeRequestById_returnsOptionalFromRepository() {
        ExchangeRequest request = new ExchangeRequest();
        when(exchangeRequestRepository.findById(55L)).thenReturn(Optional.of(request));

        Optional<ExchangeRequest> result = exchangeRequestService.getExchangeRequestById(55L);

        assertTrue(result.isPresent());
        assertSame(request, result.get());
    }

    @Test
    void hasExistingPendingRequest_returns_true_when_pending_request_exists() {
        User buyer = userWithId(1L);
        User otherBuyer = userWithId(2L);
        BookListing listing = listingWithSeller(5L, userWithId(99L));

        ExchangeRequest pending = requestWithStatus(listing, buyer, RequestStatus.PENDING);
        ExchangeRequest rejected = requestWithStatus(listing, otherBuyer, RequestStatus.REJECTED);

        when(exchangeRequestRepository.findByListingId(5L)).thenReturn(List.of(pending, rejected));

        assertTrue(exchangeRequestService.hasExistingPendingRequest(5L, buyer));
    }

    @Test
    void hasExistingPendingRequest_returns_false_when_no_matching_pending_request() {
        User buyer = userWithId(1L);
        BookListing listing = listingWithSeller(5L, userWithId(99L));

        ExchangeRequest cancelled = requestWithStatus(listing, buyer, RequestStatus.CANCELLED);

        when(exchangeRequestRepository.findByListingId(5L)).thenReturn(List.of(cancelled));

        assertFalse(exchangeRequestService.hasExistingPendingRequest(5L, buyer));
    }

    @Test
    void createExchangeRequest_succeeds_and_trims_message() {
        BookListing listing = listingWithSeller(9L, userWithId(7L));
        User buyer = userWithId(2L);

        when(bookListingRepository.findById(9L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(invocation -> {
            ExchangeRequest saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        ExchangeRequest saved = exchangeRequestService.createExchangeRequest(9L, 2L, "  Please accept  ");

        assertEquals(101L, saved.getId());
        assertSame(listing, saved.getListing());
        assertSame(buyer, saved.getBuyer());
        assertEquals("Please accept", saved.getMessage());
        assertEquals(RequestStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());

        verify(exchangeRequestRepository).save(requestCaptor.capture());
        assertEquals("Please accept", requestCaptor.getValue().getMessage());
    }

    @Test
    void createExchangeRequest_fails_when_listing_not_found() {
        when(bookListingRepository.findById(9L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> exchangeRequestService.createExchangeRequest(9L, 2L, "note")
        );

        assertEquals("Book listing not found", exception.getMessage());
        verify(userRepository, never()).findById(any(Long.class));
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    @Test
    void createExchangeRequest_fails_when_buyer_not_found() {
        BookListing listing = listingWithSeller(9L, userWithId(7L));

        when(bookListingRepository.findById(9L)).thenReturn(Optional.of(listing));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> exchangeRequestService.createExchangeRequest(9L, 2L, "note")
        );

        assertEquals("Buyer not found", exception.getMessage());
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    @Test
    void acceptExchangeRequest_succeeds_for_owner_and_pending() {
        User seller = userWithId(10L);
        BookListing listing = listingWithSeller(3L, seller);
        ExchangeRequest request = requestWithStatus(listing, userWithId(20L), RequestStatus.PENDING);

        when(exchangeRequestRepository.findById(44L)).thenReturn(Optional.of(request));
        when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRequest updated = exchangeRequestService.acceptExchangeRequest(44L, seller);

        assertEquals(RequestStatus.ACCEPTED, updated.getStatus());
        verify(exchangeRequestRepository).save(request);
    }

    @Test
    void acceptExchangeRequest_fails_when_request_not_found() {
        when(exchangeRequestRepository.findById(44L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> exchangeRequestService.acceptExchangeRequest(44L, userWithId(10L))
        );

        assertEquals("Exchange request not found", exception.getMessage());
    }

    @Test
    void acceptExchangeRequest_fails_when_seller_not_owner() {
        User seller = userWithId(10L);
        BookListing listing = listingWithSeller(3L, userWithId(99L));
        ExchangeRequest request = requestWithStatus(listing, userWithId(20L), RequestStatus.PENDING);

        when(exchangeRequestRepository.findById(44L)).thenReturn(Optional.of(request));

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> exchangeRequestService.acceptExchangeRequest(44L, seller)
        );

        assertEquals("You do not have permission to accept this request", exception.getMessage());
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    @Test
    void acceptExchangeRequest_fails_when_status_not_pending() {
        User seller = userWithId(10L);
        BookListing listing = listingWithSeller(3L, seller);
        ExchangeRequest request = requestWithStatus(listing, userWithId(20L), RequestStatus.REJECTED);

        when(exchangeRequestRepository.findById(44L)).thenReturn(Optional.of(request));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> exchangeRequestService.acceptExchangeRequest(44L, seller)
        );

        assertEquals("Only pending requests can be accepted. Current status: REJECTED", exception.getMessage());
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    @Test
    void rejectExchangeRequest_succeeds_for_owner_and_pending() {
        User seller = userWithId(10L);
        BookListing listing = listingWithSeller(3L, seller);
        ExchangeRequest request = requestWithStatus(listing, userWithId(20L), RequestStatus.PENDING);

        when(exchangeRequestRepository.findById(12L)).thenReturn(Optional.of(request));
        when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRequest updated = exchangeRequestService.rejectExchangeRequest(12L, seller);

        assertEquals(RequestStatus.REJECTED, updated.getStatus());
        verify(exchangeRequestRepository).save(request);
    }

    @Test
    void rejectExchangeRequest_fails_when_request_not_found() {
        when(exchangeRequestRepository.findById(12L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> exchangeRequestService.rejectExchangeRequest(12L, userWithId(10L))
        );

        assertEquals("Exchange request not found", exception.getMessage());
    }

    @Test
    void rejectExchangeRequest_fails_when_seller_not_owner() {
        User seller = userWithId(10L);
        BookListing listing = listingWithSeller(3L, userWithId(99L));
        ExchangeRequest request = requestWithStatus(listing, userWithId(20L), RequestStatus.PENDING);

        when(exchangeRequestRepository.findById(12L)).thenReturn(Optional.of(request));

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> exchangeRequestService.rejectExchangeRequest(12L, seller)
        );

        assertEquals("You do not have permission to reject this request", exception.getMessage());
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    @Test
    void rejectExchangeRequest_fails_when_status_not_pending() {
        User seller = userWithId(10L);
        BookListing listing = listingWithSeller(3L, seller);
        ExchangeRequest request = requestWithStatus(listing, userWithId(20L), RequestStatus.CANCELLED);

        when(exchangeRequestRepository.findById(12L)).thenReturn(Optional.of(request));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> exchangeRequestService.rejectExchangeRequest(12L, seller)
        );

        assertEquals("Only pending requests can be rejected. Current status: CANCELLED", exception.getMessage());
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    @Test
    void cancelExchangeRequest_succeeds_for_buyer_and_pending() {
        User buyer = userWithId(10L);
        BookListing listing = listingWithSeller(3L, userWithId(99L));
        ExchangeRequest request = requestWithStatus(listing, buyer, RequestStatus.PENDING);

        when(exchangeRequestRepository.findById(81L)).thenReturn(Optional.of(request));
        when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRequest updated = exchangeRequestService.cancelExchangeRequest(81L, buyer);

        assertEquals(RequestStatus.CANCELLED, updated.getStatus());
        verify(exchangeRequestRepository).save(request);
    }

    @Test
    void cancelExchangeRequest_fails_when_request_not_found() {
        when(exchangeRequestRepository.findById(81L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> exchangeRequestService.cancelExchangeRequest(81L, userWithId(10L))
        );

        assertEquals("Exchange request not found", exception.getMessage());
    }

    @Test
    void cancelExchangeRequest_fails_when_buyer_not_owner() {
        User buyer = userWithId(10L);
        BookListing listing = listingWithSeller(3L, userWithId(99L));
        ExchangeRequest request = requestWithStatus(listing, userWithId(22L), RequestStatus.PENDING);

        when(exchangeRequestRepository.findById(81L)).thenReturn(Optional.of(request));

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> exchangeRequestService.cancelExchangeRequest(81L, buyer)
        );

        assertEquals("You can only cancel your own requests", exception.getMessage());
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    @Test
    void cancelExchangeRequest_fails_when_status_not_pending() {
        User buyer = userWithId(10L);
        BookListing listing = listingWithSeller(3L, userWithId(99L));
        ExchangeRequest request = requestWithStatus(listing, buyer, RequestStatus.REJECTED);

        when(exchangeRequestRepository.findById(81L)).thenReturn(Optional.of(request));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> exchangeRequestService.cancelExchangeRequest(81L, buyer)
        );

        assertEquals("Only pending requests can be cancelled. Current status: REJECTED", exception.getMessage());
        verify(exchangeRequestRepository, never()).save(any(ExchangeRequest.class));
    }

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private BookListing listingWithSeller(Long listingId, User seller) {
        BookListing listing = new BookListing();
        listing.setId(listingId);
        listing.setSeller(seller);
        listing.setBook(new Book());
        return listing;
    }

    private ExchangeRequest requestWithStatus(BookListing listing, User buyer, RequestStatus status) {
        ExchangeRequest request = new ExchangeRequest();
        request.setListing(listing);
        request.setBuyer(buyer);
        request.setStatus(status);
        return request;
    }
}

