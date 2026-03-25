package com.example.bookexchange.service;

import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ListingStatus;
import com.example.bookexchange.repository.BookListingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminBookService {
    private final BookListingRepository bookListingRepository;

    public AdminBookService(BookListingRepository bookListingRepository) {
        this.bookListingRepository = bookListingRepository;
    }

    public List<BookListing> getAllListings() {
        return bookListingRepository.findAll();
    }

    public Optional<BookListing> updateStatusIfAllowed(Long listingId, ListingStatus newStatus) {
        if (newStatus == null) {
            return Optional.empty();
        }

        return bookListingRepository.findById(listingId).map(listing -> {
            ListingStatus current = listing.getStatus();
            boolean allowChange = false;
            if (current == ListingStatus.AVAILABLE) {
                allowChange = newStatus == ListingStatus.RESERVED
                        || newStatus == ListingStatus.SOLD;
            } else if (newStatus == ListingStatus.AVAILABLE) {
                allowChange = current == ListingStatus.RESERVED || current == ListingStatus.SOLD;
            }

            if (allowChange && current != newStatus) {
                listing.setStatus(newStatus);
                return bookListingRepository.save(listing);
            }
            return listing;
        });
    }
}
