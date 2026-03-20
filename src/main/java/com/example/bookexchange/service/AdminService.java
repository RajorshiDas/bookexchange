package com.example.bookexchange.service;

import com.example.bookexchange.dto.AdminDashboardMetrics;
import com.example.bookexchange.entity.BookListing;
import com.example.bookexchange.entity.ExchangeRequest;
import com.example.bookexchange.entity.RequestStatus;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.repository.BookListingRepository;
import com.example.bookexchange.repository.ExchangeRequestRepository;
import com.example.bookexchange.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final BookListingRepository bookListingRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;

    public AdminService(UserRepository userRepository,
                        BookListingRepository bookListingRepository,
                        ExchangeRequestRepository exchangeRequestRepository) {
        this.userRepository = userRepository;
        this.bookListingRepository = bookListingRepository;
        this.exchangeRequestRepository = exchangeRequestRepository;
    }

    public AdminDashboardMetrics getDashboardMetrics() {
        long totalUsers = userRepository.count();
        long totalListings = bookListingRepository.count();
        long totalRequests = exchangeRequestRepository.count();
        long pendingRequests = exchangeRequestRepository.countByStatus(RequestStatus.PENDING);

        return new AdminDashboardMetrics(totalUsers, totalListings, totalRequests, pendingRequests);
    }

    public List<User> getRecentUsers() {
        return userRepository.findTop5ByOrderByIdDesc();
    }

    public List<BookListing> getRecentListings() {
        return bookListingRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public List<ExchangeRequest> getRecentRequests() {
        return exchangeRequestRepository.findTop5ByOrderByCreatedAtDesc();
    }
}
