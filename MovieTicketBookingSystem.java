
/*
 * For detailed explanations, system design breakdown, and the UML diagram,
 * check out my medium article at: https://medium.com/@techiecontent/day-8-design-a-movie-ticket-booking-system-java-code-b1d2ee202e64
 *
/


import java.util.*;
import java.time.LocalDateTime;

// ENUMS
enum SeatType { REGULAR, VIP }
enum SeatStatus { AVAILABLE, HELD, BOOKED }
enum BookingStatus { CONFIRMED, CANCELLED }
enum PaymentStatus { PENDING, SUCCESS, FAILED }

// USER HIERARCHY
class User {
    protected String userId;
    protected String name;
    protected String email;
    
    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public List<Movie> searchMovie(List<Movie> movies, String title) {
        List<Movie> found = new ArrayList<>();
        for (Movie m : movies)
            if (m.getTitle().toLowerCase().contains(title.toLowerCase()))
                found.add(m);
        return found;
    }

    public Booking bookTicket(Show show, List<Seat> requestedSeats) {
        // Try to lock all requested seats
        for (Seat seat : requestedSeats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                System.out.println("Seat " + seat.getSeatNumber() + " is not available.");
                return null;
            }
        }
        // Lock all seats
        for (Seat seat : requestedSeats) {
            seat.setStatus(SeatStatus.BOOKED);
        }
        // Create payment (for demo, always success)
        Payment payment = new Payment(UUID.randomUUID().toString(), PaymentStatus.SUCCESS, requestedSeats.size()*100, LocalDateTime.now());
        Booking booking = new Booking(UUID.randomUUID().toString(), this, show, requestedSeats, BookingStatus.CONFIRMED, payment);
        show.addBooking(booking);
        return booking;
    }

    public String getName() { return name; }
}

class Admin extends User {
    public Admin(String userId, String name, String email) {
        super(userId, name, email);
    }

    public void addMovie(List<Movie> catalog, Movie movie) {
        catalog.add(movie);
    }
    public void addShow(Screen screen, Show show) {
        screen.addShow(show);
    }
}

// MOVIE, HALLS, SCREENS, SHOWS
class Movie {
    private String movieId;
    private String title;
    private String genre;
    private int durationMins;

    public Movie(String movieId, String title, String genre, int durationMins) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.durationMins = durationMins;
    }

    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public int getDurationMins() { return durationMins; }
}

class CinemaHall {
    private String hallId;
    private String name;
    private List<Screen> screens;

    public CinemaHall(String hallId, String name) {
        this.hallId = hallId;
        this.name = name;
        this.screens = new ArrayList<>();
    }

    public void addScreen(Screen screen) {
        this.screens.add(screen);
    }

    public List<Screen> getScreens() { return screens; }
    public String getName() { return name; }
}

class Screen {
    private String screenId;
    private String name;
    private int totalSeats;
    private List<Seat> seats;
    private List<Show> shows;

    public Screen(String screenId, String name, int totalSeats) {
        this.screenId = screenId;
        this.name = name;
        this.totalSeats = totalSeats;
        this.seats = new ArrayList<>();
        this.shows = new ArrayList<>();
        for (int i = 1; i <= totalSeats; i++) {
            SeatType st = (i <= totalSeats/5) ? SeatType.VIP : SeatType.REGULAR;
            seats.add(new Seat(screenId + "-" + i, st));
        }
    }

    public void addShow(Show show) {
        this.shows.add(show);
    }

    public List<Seat> getSeats() { return seats; }
    public List<Show> getShows() { return shows; }
    public String getName() { return name; }
}

class Show {
    private String showId;
    private Movie movie;
    private Screen screen;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Seat> seats; // Each show gets its own seat objects.
    private List<Booking> bookings;

    public Show(String showId, Movie movie, Screen screen, LocalDateTime startTime, LocalDateTime endTime) {
        this.showId = showId;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.endTime = endTime;
        // Deep copy seats for this show
        this.seats = new ArrayList<>();
        for (Seat seat : screen.getSeats()) {
            this.seats.add(new Seat(seat.getSeatNumber(), seat.getType()));
        }
        bookings = new ArrayList<>();
    }

    public List<Seat> getSeats() { return seats; }
    public Movie getMovie() { return movie; }
    public String getShowId() { return showId; }
    public String getMovieTitle() { return movie.getTitle(); }
    public String getScreenName() { return screen.getName(); }

    public void addBooking(Booking booking) {
        bookings.add(booking);
    }

    public void printAvailableSeats() {
        System.out.println("Available seats for show " + showId + " (" + movie.getTitle() + "):");
        for (Seat seat : seats)
            if (seat.getStatus() == SeatStatus.AVAILABLE)
                System.out.print(seat.getSeatNumber() + " ");
        System.out.println();
    }
}

// SEATS, BOOKING, PAYMENT
class Seat {
    private String seatNumber;
    private SeatType type;
    private SeatStatus status;

    public Seat(String seatNumber, SeatType type) {
        this.seatNumber = seatNumber;
        this.type = type;
        this.status = SeatStatus.AVAILABLE;
    }

    public String getSeatNumber() { return seatNumber; }
    public SeatType getType() { return type; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus s) { status = s; }
}

class Booking {
    private String bookingId;
    private User user;
    private Show show;
    private List<Seat> seats;
    private BookingStatus status;
    private Payment payment;

    public Booking(String bookingId, User user, Show show, List<Seat> seats, BookingStatus status, Payment payment) {
        this.bookingId = bookingId;
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.status = status;
        this.payment = payment;
    }

    public String getBookingId() { return bookingId; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus s) { status = s; }
    public List<Seat> getSeats() { return seats; }

    public void cancel() {
        setStatus(BookingStatus.CANCELLED);
        for (Seat seat : seats) seat.setStatus(SeatStatus.AVAILABLE);
        System.out.println("Booking " + bookingId + " cancelled; seats released.");
    }
}

class Payment {
    private String paymentId;
    private PaymentStatus status;
    private double amount;
    private LocalDateTime paymentTime;

    public Payment(String paymentId, PaymentStatus status, double amount, LocalDateTime paymentTime) {
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
        this.paymentTime = paymentTime;
    }

    public PaymentStatus getStatus() { return status; }
    public double getAmount() { return amount; }
}

// DEMO APPLICATION
public class MovieBookingDemo {
    public static void main(String[] args) {
        // MOVIE CATALOG AND ADMIN SETUP
        List<Movie> cat = new ArrayList<>();
        Admin admin = new Admin("admin1", "CinemaAdmin", "admin@cinema.com");
        Movie m1 = new Movie("mov1", "Interstellar", "Sci-Fi", 169);
        Movie m2 = new Movie("mov2", "Coco", "Animation", 100);
        admin.addMovie(cat, m1); admin.addMovie(cat, m2);

        // CINEMAHALL & SCREENS SETUP
        CinemaHall hall = new CinemaHall("hall1", "Grand Multiplex");
        Screen s1 = new Screen("S1", "Screen 1", 20); // 20 seats
        hall.addScreen(s1);

        // ADD SHOWS TO SCREEN
        Show show1 = new Show("show1", m1, s1, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(3));
        Show show2 = new Show("show2", m2, s1, LocalDateTime.now().plusDays(1).plusHours(4), LocalDateTime.now().plusDays(1).plusHours(6));
        admin.addShow(s1, show1);
        admin.addShow(s1, show2);

        // USER SEARCH AND BOOKING
        User user = new User("u1", "Alice", "alice@email.com");
        System.out.println("Available movies:");
        for (Movie mv : user.searchMovie(cat, "")) {
            System.out.println(" " + mv.getTitle() + " (" + mv.getGenre() + ")");
        }
        System.out.println();

        // Search for 'Interstellar', book 2 seats in show1
        System.out.println(user.getName() + " booking seats in Interstellar's show.");
        show1.printAvailableSeats();
        List<Seat> available = show1.getSeats();

        // Collect 2 available seats
        List<Seat> mySeats = new ArrayList<>();
        for (Seat seat : available)
            if (seat.getStatus() == SeatStatus.AVAILABLE && mySeats.size() < 2)
                mySeats.add(seat);

        if (mySeats.size() == 2) {
            Booking myBooking = user.bookTicket(show1, mySeats);
            if (myBooking != null)
                System.out.println("Booking successful: " + myBooking.getBookingId());
        }
        show1.printAvailableSeats();

        // Cancel booking (demonstrate cancellation)
        if (mySeats.size() == 2) {
            Booking cancellationDemo = user.bookTicket(show1, mySeats);
            if (cancellationDemo != null) {
                cancellationDemo.cancel();
                show1.printAvailableSeats();
            }
        }
    }
}
