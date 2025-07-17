package com.redmath.bookmanagement.books;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;

    public Book createBook(Book book) {
        log.info("Creating book with ISBN: {}", book.getIsbn());
        return bookRepository.save(book);
    }

    public Book getBookById(Long id) {
        log.info("Fetching book with ID: {}", id);
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with ID: " + id));
    }

    public List<Book> getAllBooks() {
        log.info("Fetching all books");
        return bookRepository.findAll();
    }

    public Book updateBook(Long id, Book updatedBook) {
        log.info("Updating book with ID: {}", id);
        Book existingBook = getBookById(id);

        existingBook.setTitle(updatedBook.getTitle());
        existingBook.setAuthor(updatedBook.getAuthor());
        existingBook.setIsbn(updatedBook.getIsbn());
        existingBook.setPublishedYear(updatedBook.getPublishedYear());

        return bookRepository.save(existingBook);
    }

    public void deleteBook(Long id) {
        log.info("Deleting book with ID: {}", id);
        Book book = getBookById(id);
        bookRepository.delete(book);
    }
}
