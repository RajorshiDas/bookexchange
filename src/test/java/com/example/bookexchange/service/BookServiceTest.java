package com.example.bookexchange.service;

import com.example.bookexchange.entity.Book;
import com.example.bookexchange.entity.BookCondition;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ListingStatus;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.ForbiddenOperationException;
import com.example.bookexchange.exception.ResourceNotFoundException;
import com.example.bookexchange.repository.BookListingRepository;
import com.example.bookexchange.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookListingRepository bookListingRepository;

    @InjectMocks
    private BookService bookService;

    @Captor
    private ArgumentCaptor<Book> bookCaptor;

    @Captor
    private ArgumentCaptor<BookListing> listingCaptor;

    @Test
    void getSellerBooks_returnsRepositoryResults() {
        User seller = sellerWithId(10L);
        List<BookListing> listings = List.of(listingWithSeller(1L, seller, bookWithId(100L)));

        when(bookListingRepository.findBySeller(seller)).thenReturn(listings);

        List<BookListing> result = bookService.getSellerBooks(seller);

        assertSame(listings, result);
        verify(bookListingRepository).findBySeller(seller);
    }

    @Test
    void getBookListingById_returnsOptionalFromRepository() {
        BookListing listing = listingWithSeller(1L, sellerWithId(10L), bookWithId(100L));
        when(bookListingRepository.findById(1L)).thenReturn(Optional.of(listing));

        Optional<BookListing> result = bookService.getBookListingById(1L);

        assertSame(listing, result.orElseThrow());
    }

    @Test
    void verifySellerOwnership_succeeds_for_owner() {
        User seller = sellerWithId(10L);
        BookListing listing = listingWithSeller(1L, seller, bookWithId(100L));

        when(bookListingRepository.findById(1L)).thenReturn(Optional.of(listing));

        assertDoesNotThrow(() -> bookService.verifySellerOwnership(1L, seller));
    }

    @Test
    void verifySellerOwnership_fails_when_listing_missing() {
        when(bookListingRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.verifySellerOwnership(1L, sellerWithId(10L))
        );

        assertEquals("Book listing not found", exception.getMessage());
    }

    @Test
    void verifySellerOwnership_fails_when_seller_not_owner() {
        User seller = sellerWithId(10L);
        User otherSeller = sellerWithId(11L);
        BookListing listing = listingWithSeller(1L, otherSeller, bookWithId(100L));

        when(bookListingRepository.findById(1L)).thenReturn(Optional.of(listing));

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> bookService.verifySellerOwnership(1L, seller)
        );

        assertEquals("You do not have permission to modify this book listing", exception.getMessage());
    }

    @Test
    void addBook_succeeds_with_valid_data() {
        User seller = sellerWithId(10L);

        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });
        when(bookListingRepository.save(any(BookListing.class))).thenAnswer(invocation -> {
            BookListing saved = invocation.getArgument(0);
            saved.setId(300L);
            return saved;
        });

        BookListing listing = bookService.addBook(
                " Effective Java ",
                " Joshua Bloch ",
                " 9780134685991 ",
                " Programming ",
                " Classic Java guide ",
                BookCondition.GOOD,
                new BigDecimal("45.00"),
                seller
        );

        assertEquals(300L, listing.getId());
        assertNotNull(listing.getCreatedAt());
        assertEquals(ListingStatus.AVAILABLE, listing.getStatus());
        assertSame(seller, listing.getSeller());
        assertEquals(new BigDecimal("45.00"), listing.getPrice());

        verify(bookRepository).save(bookCaptor.capture());
        Book savedBook = bookCaptor.getValue();
        assertEquals("Effective Java", savedBook.getTitle());
        assertEquals("Joshua Bloch", savedBook.getAuthor());
        assertEquals("9780134685991", savedBook.getIsbn());
        assertEquals("Programming", savedBook.getCategory());
        assertEquals("Classic Java guide", savedBook.getDescription());
        assertEquals(BookCondition.GOOD, savedBook.getCondition());

        verify(bookListingRepository).save(listingCaptor.capture());
        assertSame(savedBook, listingCaptor.getValue().getBook());
    }

    @Test
    void addBook_fails_when_title_missing() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookService.addBook(
                        "  ",
                        "Author",
                        null,
                        null,
                        null,
                        BookCondition.NEW,
                        new BigDecimal("10.00"),
                        sellerWithId(10L)
                )
        );

        assertEquals("Book title is required", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
        verify(bookListingRepository, never()).save(any(BookListing.class));
    }

    @Test
    void addBook_fails_when_price_invalid() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookService.addBook(
                        "Title",
                        "Author",
                        null,
                        null,
                        null,
                        BookCondition.NEW,
                        new BigDecimal("-1"),
                        sellerWithId(10L)
                )
        );

        assertEquals("Valid price is required", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
        verify(bookListingRepository, never()).save(any(BookListing.class));
    }

    @Test
    void editBook_succeeds_for_owner_and_valid_data() {
        User seller = sellerWithId(10L);
        Book book = bookWithId(200L);
        BookListing listing = listingWithSeller(1L, seller, book);

        when(bookListingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookListingRepository.save(any(BookListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookListing updated = bookService.editBook(
                1L,
                "Clean Code",
                "Robert C. Martin",
                "9780132350884",
                "Programming",
                "Craftsmanship",
                BookCondition.LIKE_NEW,
                new BigDecimal("55.00"),
                seller
        );

        assertSame(listing, updated);
        assertEquals(new BigDecimal("55.00"), updated.getPrice());
        assertEquals("Clean Code", book.getTitle());
        assertEquals("Robert C. Martin", book.getAuthor());
        assertEquals("9780132350884", book.getIsbn());
        assertEquals(BookCondition.LIKE_NEW, book.getCondition());
    }

    @Test
    void editBook_fails_when_title_missing() {
        User seller = sellerWithId(10L);
        BookListing listing = listingWithSeller(1L, seller, bookWithId(200L));

        when(bookListingRepository.findById(1L)).thenReturn(Optional.of(listing));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookService.editBook(
                        1L,
                        " ",
                        "Author",
                        null,
                        null,
                        null,
                        BookCondition.GOOD,
                        new BigDecimal("20.00"),
                        seller
                )
        );

        assertEquals("Book title is required", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
        verify(bookListingRepository, never()).save(any(BookListing.class));
    }

    @Test
    void deleteBook_succeeds_for_owner() {
        User seller = sellerWithId(10L);
        Book book = bookWithId(200L);
        BookListing listing = listingWithSeller(1L, seller, book);

        when(bookListingRepository.findById(1L)).thenReturn(Optional.of(listing), Optional.of(listing));

        bookService.deleteBook(1L, seller);

        verify(bookListingRepository).deleteById(1L);
        verify(bookRepository).deleteById(200L);
    }

    @Test
    void deleteBook_fails_when_seller_not_owner() {
        User seller = sellerWithId(10L);
        BookListing listing = listingWithSeller(1L, sellerWithId(11L), bookWithId(200L));

        when(bookListingRepository.findById(1L)).thenReturn(Optional.of(listing));

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> bookService.deleteBook(1L, seller)
        );

        assertEquals("You do not have permission to modify this book listing", exception.getMessage());
        verify(bookListingRepository, never()).deleteById(1L);
        verify(bookRepository, never()).deleteById(any(Long.class));
    }

    private User sellerWithId(Long id) {
        User seller = new User();
        seller.setId(id);
        seller.setUsername("seller" + id);
        seller.setEmail("seller" + id + "@example.com");
        return seller;
    }

    private Book bookWithId(Long id) {
        Book book = new Book();
        book.setId(id);
        book.setTitle("Sample Title");
        book.setAuthor("Sample Author");
        book.setIsbn("ISBN" + id);
        book.setCategory("Sample Category");
        book.setDescription("Sample Description");
        book.setCondition(BookCondition.GOOD);
        return book;
    }

    private BookListing listingWithSeller(Long listingId, User seller, Book book) {
        BookListing listing = new BookListing();
        listing.setId(listingId);
        listing.setSeller(seller);
        listing.setBook(book);
        listing.setStatus(ListingStatus.AVAILABLE);
        listing.setPrice(new BigDecimal("30.00"));
        return listing;
    }
}

