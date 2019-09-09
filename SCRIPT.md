## DEMO 1
1. Simple `Mono`
    ```
    Mono.just("Hello, AJUG").subscribe(AjugReactiveDemoApplication::log);
    ```
2. Simple `Flux`
    ```
    Flux.fromIterable(Arrays.asList("Hello", "AJUG")).subscribe(AjugReactiveDemoApplication::log);
    ```
3. Map `Mono` to `Flux`
    ```
    String txt = "Reactive Hello World, From Conspect And The AJUG On This 5th Of September In The Beautiful Almere."
    Mono.just(txt)
        .flatMapMany(s -> Flux.fromArray(s.split(" ")))
        .map(String::toUpperCase)
        .subscribe(AjugReactiveDemoApplication::log);
    ```

4. Add ` .delayElements(Duration.ofMillis(5))`

## DEMO 2
1. Show `Book` class
    ```java
    package nl.conspect.ajugreactivedemo.books;
    
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    import java.util.Objects;
    
    import org.springframework.data.annotation.Id;
    import org.springframework.data.mongodb.core.mapping.Document;
    
    @Document
    public class Book {
    
        @Id
        private String isbn;
        private String title;
        private List<String> authors = new ArrayList<>();
    
        public Book() {}
    
        public Book(String isbn, String title, List<String> authors) {
            this.isbn = isbn;
            this.title = title;
            this.authors = authors;
        }
    
        public String getIsbn() {
            return this.isbn;
        }
    
        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }
    
        public String getTitle() {
            return this.title;
        }
    
        public void setTitle(String title) {
            this.title = title;
        }
    
        public List<String> getAuthors() {
            return Collections.unmodifiableList(this.authors);
        }
    
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Book book = (Book) o;
            return Objects.equals(isbn, book.isbn);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(isbn);
        }
    
        @Override
        public String toString() {
            return String.format("Book (isbn=%s, title=%s, authors=%s)", this.isbn, this.title, this.authors);
        }
    }

    ```
1. Add `BookRepository`
    ```java
    public interface BookRepository extends ReactiveMongoRepository<Book, String> {
    ```
3. Add `ApplicationRunner`
    
    ```java
    @Bean
    public ApplicationRunner initializer(BookRepository books) {
        return args -> {
            List<Book> boeken = asList(
                    new Book("9781430241560", "Pro Spring MVC", asList("Marten Deinum", "Koen Serneels", "Colin Yates")),
                    new Book("9781484239636", "Spring Boot 2 Recipes", asList("Marten Deinum")),
                    new Book("9781484227909", "Spring 5 Recipes", asList("Marten Deinum", "Daniel Rubio, Josh Long")));
    
            books.saveAll(Flux.fromIterable(boeken))
                    .subscribe(AjugReactiveDemoApplication::log);
    
        };
    }
    ```

4. Run application and show log
5. Add `BookController`
    ```java
   @RestController
   @RequestMapping("/books")
   public class BookController {
    
       private final BookRepository books;
        
        public BookController(BookRepository books) {
            this.books = books;
        }
        
        @GetMapping(value = "/books", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
        public Flux<Book> list() {
            return books.findAll().delayElements(Duration.ofMillis(250));
        }
    }    
    ```
6. Execute `http :8080/books --stream` on command line
7. Add method to get single book to `BookController`
    ```java
    
    ```
8. Execute `http :8080/books/1234567890` to show 404
9. Execute `http :8080/books/9781430241560` to show actual result
10. Add method to add book to `BookController`
    ```java
    @PostMapping
    public Mono<ResponseEntity<Book>> create(@RequestBody Book book) {
        return this.books.save(book).map(b ->
                ResponseEntity.created(URI.create("/books/" + b.getIsbn())).contentType(MediaType.APPLICATION_JSON).build());
    }
    ```
 11. Execute `http POST :8080/books isbn=9781430259091 title="Spring Recipes" authors[]="Marten Deinum"`
 12. Execute `http :8080/books` to show list with added book.
 
 ## Demo 3
 1. Remove `BookController`
 2. Add `RouterFunction` as `@Bean`
    ```java
    @Bean
    public RouterFunction<ServerResponse> router(BookRepository books) {
        return RouterFunctions.route()
                .GET("/books", req -> ServerResponse.ok().body(books.findAll().delayElements(Duration.ofMillis(250)), Book.class))
                .GET("/books/{isbn}", req ->
                        books.findById(req.pathVariable("isbn"))
                                .flatMap(b -> ServerResponse.ok().body(b, Book.class))
                                .switchIfEmpty(ServerResponse.notFound().build()))
                .POST("/books", req -> req.bodyToMono(Book.class)
                        .flatMap(b -> ServerResponse.created(URI.create("/books" + b.getIsbn())).body(
                                fromPublisher(books.save(b), Book.class)))).build();
    }
    ```
    
3. Reuse previous HTTPIE commands to list/get/add book
4. Move implementation to functional methods
    ```java
    @Service
    public class BookService {
    
        private final BookRepository books;
    
        public BookService(BookRepository books) {
            this.books = books;
        }
    
        public Mono<ServerResponse> list(ServerRequest req) {
            return ServerResponse.ok().body(fromPublisher(books.findAll(), Book.class));
        }
    
        public Mono<ServerResponse> get(ServerRequest req) {
            String isbn = req.pathVariable("isbn");
            Mono<Book> result = books.findById(isbn);
            return result
                    .flatMap(b -> ServerResponse.ok().body(b, Book.class))
                    .switchIfEmpty(ServerResponse.notFound().build());
        }
    
        public Mono<ServerResponse> create(ServerRequest req) {
            Mono<Book> book = req.bodyToMono(Book.class);
            return book.flatMap(
                    b -> ServerResponse.created(URI.create("/books" + b.getIsbn()))
                    .body(fromPublisher(books.save(b), Book.class)));
        }
    }
    ```

5. Rewrite router function
    ```java
    @Bean
    public RouterFunction<ServerResponse> router(BookService books) {
        return RouterFunctions.route()
                .GET("/books", books::list)
                .GET("/books/{isbn}", books::get)
                .POST("/books", books::create).build();
    }
    ```
   
## DEMO 4
1. Add `@EnableTransactionManagement` to application class
2. Add `ReactiveTransactionManager`
    ```java
    @Bean
    public ReactiveTransactionManager transactionManager(ReactiveMongoDatabaseFactory mongo) {
        return new ReactiveMongoTransactionManager(mongo);
    }
    ```
3. Make create method in `BookService` `@Transactional`. 
