package com.example.bookexchange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a book listed on the platform by a seller.
 */
@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    // ISBN is optional but useful for identification
    @Column(unique = true)
    private String isbn;

    private String category;

    // Short description of the book
    @Column(length = 2000)
    private String description;

    // Physical condition of the book
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookCondition condition;
}
