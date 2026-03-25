package com.example.bookexchange.dto.api;

import com.example.bookexchange.entity.BookCondition;
import com.example.bookexchange.entity.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookListingResponseDto {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private String description;
    private BookCondition condition;
    private BigDecimal price;
    private ListingStatus status;
    private LocalDateTime createdAt;
    private Long sellerId;
    private String sellerUsername;
    private String sellerEmail;
}

