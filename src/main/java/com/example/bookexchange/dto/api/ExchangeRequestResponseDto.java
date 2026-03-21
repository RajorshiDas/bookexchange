package com.example.bookexchange.dto.api;

import com.example.bookexchange.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequestResponseDto {

    private Long id;
    private Long listingId;
    private String bookTitle;
    private Long buyerId;
    private String buyerUsername;
    private String buyerEmail;
    private Long sellerId;
    private String sellerUsername;
    private String sellerEmail;
    private String message;
    private RequestStatus status;
    private LocalDateTime createdAt;
}

