## Seller CRUD Implementation - Setup Guide

### Overview
This implementation provides complete seller CRUD functionality for the Book Exchange platform. Sellers can now:
- ✅ View all their listed books
- ✅ Add new books to the marketplace
- ✅ Edit existing book details and prices
- ✅ Delete books from their listings

### Components Implemented

#### 1. Service Layer (`BookService.java`)
**Location:** `src/main/java/com/example/bookexchange/service/BookService.java`

**Key Methods:**
- `getSellerBooks(User seller)` - Fetch all books for a specific seller
- `addBook(...)` - Create a new book and listing
- `editBook(...)` - Update an existing book and its price
- `deleteBook(...)` - Remove a book listing and associated book
- `verifySellerOwnership(Long bookListingId, User seller)` - Ensure seller owns the book

**Security Features:**
- Ownership verification before edit/delete operations
- Throws `IllegalAccessError` if seller doesn't own the book
- Uses `@Transactional` for data consistency

#### 2. Controller Layer (`SellerController.java`)
**Location:** `src/main/java/com/example/bookexchange/controller/SellerController.java`

**Endpoints:**
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/seller/books` | GET | List all seller's books |
| `/seller/books/add` | GET | Show add book form |
| `/seller/books/add` | POST | Process new book submission |
| `/seller/books/edit/{id}` | GET | Show edit form |
| `/seller/books/edit/{id}` | POST | Process book update |
| `/seller/books/delete/{id}` | GET | Show delete confirmation |
| `/seller/books/delete/{id}` | POST | Execute deletion |

**Security Features:**
- `@RequestMapping("/seller")` - All endpoints require SELLER role
- `getCurrentUser(Authentication)` - Extract logged-in user from Spring Security
- Ownership verification before allowing edits/deletes
- Proper error handling with user-friendly messages

#### 3. Templates (Thymeleaf)

##### `books-list.html`
- Displays grid of seller's books
- Shows book title, author, condition, price, status
- Action buttons: Edit, Delete
- "Add New Book" button in header
- Empty state message if no books

##### `book-add.html`
- Form to add new book
- Fields: Title*, Author*, ISBN, Category, Description, Condition*, Price*
- Condition dropdown populated from `BookCondition` enum
- Form validation on submit
- Cancel button to return to list

##### `book-edit.html`
- Form to edit existing book
- Pre-populated with current book data
- Display of creation date and current status
- Same fields as add form
- Preserves book listing status

##### `book-delete-confirm.html`
- Confirmation page before deletion
- Display book details to confirm correct book
- Warning message about permanent deletion
- Checkbox confirmation required to enable delete button
- JavaScript to enable/disable delete button based on checkbox

### Security & Authorization

#### Role-Based Access
- Protected by Spring Security (requires SELLER role)
- Configured in `SecurityConfig.java`:
  ```
  .requestMatchers("/seller/**").hasRole("SELLER")
  ```

#### Seller Ownership Verification
- All edit/delete operations verify seller ownership
- Method: `BookService.verifySellerOwnership(Long bookListingId, User seller)`
- Throws `IllegalAccessError` if unauthorized

#### User Extraction
- Uses Spring Security's `Authentication` object
- Retrieves username from authentication principal
- Queries database for User entity
- Ensures seller can only access their own books

### Data Model

**Related Entities:**
- `Book` - Core book information (title, author, ISBN, category, description, condition)
- `BookListing` - Seller's listing (price, status, seller reference, creation date)
- `User` - Seller information
- `BookCondition` - Enum (NEW, LIKE_NEW, GOOD, FAIR, POOR)
- `ListingStatus` - Enum (AVAILABLE, RESERVED, EXCHANGED, SOLD)

**Key Relationships:**
- `BookListing` → `Book` (OneToOne with cascade delete)
- `BookListing` → `User` (ManyToOne - seller reference)

### Running the Application

#### Prerequisites
- Java 17 or higher
- PostgreSQL database running
- Maven or use the included `mvnw` wrapper

#### Database Setup
Database credentials are configured in `application.properties`:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/bookexchangedb
spring.datasource.username=megha
spring.datasource.password=meghapostgresql123
```

Database tables are auto-created with `spring.jpa.hibernate.ddl-auto=update`

#### Start the Application

**Option 1: Using Maven Wrapper (Windows)**
```powershell
cd "C:\path\to\bookexchange"
.\mvnw.cmd spring-boot:run
```

**Option 2: Using IDE (IntelliJ IDEA)**
1. Open project in IntelliJ
2. Right-click `BookexchangeApplication.java`
3. Select "Run"

**Option 3: Build and Run JAR**
```powershell
.\mvnw.cmd clean package
java -jar target/bookexchange-0.0.1-SNAPSHOT.jar
```

#### Access the Application
- **URL:** http://localhost:8081
- **Server Port:** 8081 (configured in `application.properties`)

### Testing Seller CRUD Workflow

#### Test Scenario 1: Add a Book

1. Register a new seller account:
   - Go to http://localhost:8081/auth/register
   - Username: `seller1`
   - Email: `seller1@test.com`
   - Password: `password123`
   - Select role: **SELLER**
   - Click Register

2. Login with seller credentials:
   - Go to http://localhost:8081/auth/login
   - Username: `seller1`
   - Password: `password123`
   - You'll be redirected to seller dashboard

3. Add a book:
   - Click "My Books" in navbar or "Add Book" on dashboard
   - Fill in the form:
     - Title: "Clean Code"
     - Author: "Robert C. Martin"
     - ISBN: "978-0132350884"
     - Category: "Programming"
     - Description: "A Handbook of Agile Software Craftsmanship"
     - Condition: "GOOD"
     - Price: "29.99"
   - Click "Add Book"
   - You should see success message

4. Verify the book appears in your book list

#### Test Scenario 2: Edit a Book

1. From the books list, click "Edit" on any book
2. Modify the details:
   - Change title or price
   - Update condition if needed
3. Click "Save Changes"
4. Verify changes are reflected in the list

#### Test Scenario 3: Delete a Book

1. From the books list, click "Delete" on any book
2. Review the book details on confirmation page
3. Check the confirmation checkbox ("I want to permanently delete...")
4. Click "Delete Book"
5. Verify book is removed from list

#### Test Scenario 4: Security Verification

1. Login as `seller1` and note a book ID from URL edit link (e.g., ID=5)
2. Register another seller (`seller2`)
3. Try to access seller1's book directly:
   - http://localhost:8081/seller/books/edit/5
   - Should get error message: "Unauthorized access"
4. Try to delete seller1's book:
   - POST to `/seller/books/delete/5`
   - Should get error message: "Unauthorized access"

#### Test Scenario 5: Data Validation

1. Go to "Add New Book"
2. Leave required fields empty and try to submit
3. System should show error: "Book title is required" etc.
4. Try invalid price (negative number)
5. System should show error: "Valid price is required"

### Database Tables Created

The application automatically creates these tables:

```sql
users (id, username, email, password, first_name, last_name, role, enabled)
books (id, title, author, isbn, category, description, condition)
book_listings (id, book_id, seller_id, price, status, created_at)
exchange_requests (id, buyer_id, seller_id, ... )
```

### Troubleshooting

**Issue: Application fails to start**
- Verify PostgreSQL is running
- Check database credentials in `application.properties`
- Run `.\mvnw.cmd clean install` to rebuild dependencies

**Issue: Can't access seller pages**
- Ensure you're logged in with a SELLER role account
- Check Spring Security configuration in `SecurityConfig.java`

**Issue: Can edit/delete other sellers' books**
- This should NOT happen - verify `BookService.verifySellerOwnership()` is being called
- Check logs for `IllegalAccessError` exceptions

**Issue: Forms not submitting**
- Check browser console for JavaScript errors
- Verify Thymeleaf variables are populated (use `th:` namespace)
- Ensure POST method is specified in form

### Code Comments

All classes include comprehensive comments:
- **Class-level comments** explain overall purpose
- **Method comments** describe parameters and behavior
- **Inline comments** clarify complex logic
- **Security comments** highlight authorization checks

### File Manifest

**New Files Created:**
1. `src/main/java/com/example/bookexchange/service/BookService.java` - Service layer
2. `src/main/java/com/example/bookexchange/controller/SellerController.java` - Controller layer
3. `src/main/resources/templates/books-list.html` - List books template
4. `src/main/resources/templates/book-add.html` - Add book template
5. `src/main/resources/templates/book-edit.html` - Edit book template
6. `src/main/resources/templates/book-delete-confirm.html` - Delete confirmation template

**Modified Files:**
- None (all implementations are additive)

**Configuration Files:**
- `application.properties` - No changes needed (uses existing credentials)
- `pom.xml` - No additional dependencies needed (all existing)
- `SecurityConfig.java` - Already configured for `/seller/**` routes

### Future Enhancements

- [ ] Add file upload for book cover images
- [ ] Implement book listing status toggle (SOLD/DELISTED) without hard delete
- [ ] Add search/filter functionality for seller's books
- [ ] Implement bulk operations (delete multiple books)
- [ ] Add book listing history/analytics
- [ ] Email notifications for book sales/exchanges
- [ ] Advanced search for book marketplace
- [ ] Buyer pages for browsing and purchasing
- [ ] Admin dashboard for moderation

### Support & Documentation

- Spring Boot: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- Thymeleaf: https://www.thymeleaf.org/
- JPA/Hibernate: https://hibernate.org/orm/
- PostgreSQL: https://www.postgresql.org/

---
**Created:** March 12, 2026
**Version:** 1.0
**Status:** Ready for Testing ✅
