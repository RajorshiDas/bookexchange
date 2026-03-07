package com.example.bookexchange.repository;

import com.example.bookexchange.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Search books by title (case-insensitive)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Search books by author (case-insensitive)
    List<Book> findByAuthorContainingIgnoreCase(String author);

    // Find book by ISBN
    Optional<Book> findByIsbn(String isbn);
}
