package com.example.bookexchange.dto.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequestCreateDto {

    @NotNull
    private Long listingId;

    @NotNull
    private Long buyerId;

    @Size(max = 1000)
    private String message;
}

