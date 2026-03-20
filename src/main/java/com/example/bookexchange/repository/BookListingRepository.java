package com.example.bookexchange.repository;

import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ListingStatus;
import com.example.bookexchange.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookListingRepository extends JpaRepository<BookListing, Long> {

    List<BookListing> findBySeller(User seller);

    List<BookListing> findByStatus(ListingStatus status);

    List<BookListing> findByBook_TitleContainingIgnoreCase(String title);

    List<BookListing> findByBook_AuthorContainingIgnoreCase(String author);

    List<BookListing> findTop5ByOrderByCreatedAtDesc();
}
