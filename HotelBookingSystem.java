import java.util.*;
import java.time.*;

// ENUMS
enum RoomType { SINGLE, DOUBLE, SUITE }
enum RoomStatus { AVAILABLE, BOOKED }
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

    public List<Hotel> searchHotels(List<Hotel> hotels, String city) {
        List<Hotel> result = new ArrayList<>();
        for (Hotel hotel : hotels)
            if (hotel.getAddress().getCity().equalsIgnoreCase(city))
                result.add(hotel);
        return result;
    }

    public Booking bookRoom(Room room, LocalDate start, LocalDate end) {
        if (!room.isAvailableForDates(start, end)) {
            System.out.println("Room is not available for selected dates.");
            return null;
        }
        room.bookDates(start, end);
        Payment payment = new Payment(UUID.randomUUID().toString(), PaymentStatus.SUCCESS, 
                (room.getType() == RoomType.SUITE ? 200 : (room.getType() == RoomType.DOUBLE ? 150 : 100)),
                LocalDateTime.now());
        Booking booking = new Booking(UUID.randomUUID().toString(), this, room, start, end, BookingStatus.CONFIRMED, payment);
        room.addBooking(booking);
        return booking;
    }

    public String getName() { return name; }
}

class Admin extends User {
    public Admin(String userId, String name, String email) {
        super(userId, name, email);
    }

    public void addHotel(List<Hotel> hotelList, Hotel hotel) {
        hotelList.add(hotel);
    }

    public void addRoom(Hotel hotel, Room room) {
        hotel.addRoom(room);
    }
}

// HOTEL, ROOMS, ADDRESS
class Address {
    private String street, city, zip, country;

    public Address(String street, String city, String zip, String country) {
        this.street = street;
        this.city = city;
        this.zip = zip;
        this.country = country;
    }

    public String getCity() { return city; }
    public String toString() {
        return street + ", " + city + ", " + zip + ", " + country;
    }
}

class Hotel {
    private String hotelId;
    private String name;
    private Address address;
    private List<Room> rooms;

    public Hotel(String hotelId, String name, Address address) {
        this.hotelId = hotelId;
        this.name = name;
        this.address = address;
        this.rooms = new ArrayList<>();
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
    }

    public String getName() { return name; }
    public Address getAddress() { return address; }
    public List<Room> getRooms() { return rooms; }
}

class Room {
    private String roomNumber;
    private RoomType type;
    private RoomStatus status;
    private List<Booking> bookings;

    // For simplicity, we track dates booked as periods in DatePair objects
    private List<DatePair> bookedDates;

    public Room(String roomNumber, RoomType type) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.status = RoomStatus.AVAILABLE;
        this.bookings = new ArrayList<>();
        this.bookedDates = new ArrayList<>();
    }

    // Check if room is available for all days in the range
    public boolean isAvailableForDates(LocalDate start, LocalDate end) {
        for (DatePair bp : bookedDates) {
            if (start.isBefore(bp.end) && end.isAfter(bp.start)) // overlap
                return false;
        }
        return true;
    }

    // Reserve for date range
    public void bookDates(LocalDate start, LocalDate end) {
        bookedDates.add(new DatePair(start, end));
        if (status == RoomStatus.AVAILABLE) status = RoomStatus.BOOKED;
    }

    public void addBooking(Booking b) {
        bookings.add(b);
    }

    public void releaseDates(LocalDate start, LocalDate end) {
        bookedDates.removeIf(bp -> bp.start.equals(start) && bp.end.equals(end));
        if (bookedDates.isEmpty()) status = RoomStatus.AVAILABLE;
    }

    public String getRoomNumber() { return roomNumber; }
    public RoomType getType() { return type; }
    public RoomStatus getStatus() { return status; }

    public boolean hasBookingActiveDuring(LocalDate start, LocalDate end) {
        for (DatePair bp : bookedDates) {
            if (start.isBefore(bp.end) && end.isAfter(bp.start))
                return true;
        }
        return false;
    }
}

// For tracking a date range for a room booking
class DatePair {
    LocalDate start, end;
    public DatePair(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }
}

// BOOKING, PAYMENT
class Booking {
    private String bookingId;
    private User user;
    private Room room;
    private LocalDate startDate, endDate;
    private BookingStatus status;
    private Payment payment;

    public Booking(String bookingId, User user, Room room, LocalDate startDate, LocalDate endDate,
                   BookingStatus status, Payment payment) {
        this.bookingId = bookingId;
        this.user = user;
        this.room = room;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.payment = payment;
    }

    public String getBookingId() { return bookingId; }
    public BookingStatus getStatus() { return status; }
    public Room getRoom() { return room; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setStatus(BookingStatus s) { status = s; }

    // Cancel booking, release room for dates
    public void cancel() {
        setStatus(BookingStatus.CANCELLED);
        room.releaseDates(startDate, endDate);
        System.out.println("Booking " + bookingId + " cancelled; room released.");
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
public class HotelBookingDemo {
    public static void main(String[] args) {
        // ADMIN SETUP
        List<Hotel> allHotels = new ArrayList<>();
        Admin admin = new Admin("admin1", "HotelAdmin", "admin@hotel.com");
        Address addr1 = new Address("123 Main St", "Metropolis", "12345", "Country");
        Address addr2 = new Address("456 King Rd", "Gotham", "54321", "Country");
        Hotel h1 = new Hotel("H1", "Sunrise Hotel", addr1);
        Hotel h2 = new Hotel("H2", "Royal Palace", addr2);
        admin.addHotel(allHotels, h1);
        admin.addHotel(allHotels, h2);

        // Add rooms to hotels
        admin.addRoom(h1, new Room("101", RoomType.SINGLE));
        admin.addRoom(h1, new Room("102", RoomType.DOUBLE));
        admin.addRoom(h1, new Room("103", RoomType.SUITE));
        admin.addRoom(h2, new Room("201", RoomType.SINGLE));
        admin.addRoom(h2, new Room("202", RoomType.SUITE));

        // USER INTERACTION
        User user = new User("u1", "Bob", "bob@email.com");
        List<Hotel> availableHotels = user.searchHotels(allHotels, "Metropolis");
        System.out.println("Hotels in Metropolis:");
        for (Hotel h : availableHotels)
            System.out.println(" - " + h.getName() + " (" + h.getAddress() + ")");
        System.out.println();

        // Search for available DOUBLE rooms in the first hotel for tomorrow for 2 days
        System.out.println(user.getName() + " searching for DOUBLE room in " + h1.getName());
        List<Room> doubleRooms = new ArrayList<>();
        for (Room r : h1.getRooms())
            if (r.getType() == RoomType.DOUBLE)
                doubleRooms.add(r);

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        Room selectedRoom = null;
        for (Room r : doubleRooms) {
            if (r.isAvailableForDates(start, end)) {
                selectedRoom = r; break;
            }
        }

        if (selectedRoom != null) {
            Booking booking = user.bookRoom(selectedRoom, start, end);
            if (booking != null)
                System.out.println("Room " + selectedRoom.getRoomNumber() + " booked! Booking ID: " + booking.getBookingId());
        } else {
            System.out.println("No double rooms available for selected dates.");
        }

        // Attempt cancellation
        if (selectedRoom != null) {
            Booking booking = user.bookRoom(selectedRoom, start, end);
            if (booking != null) {
                booking.cancel();
            }
        }

        // List all bookings for h1
        System.out.println("\nCurrent bookings in " + h1.getName() + ":");
        for (Room r : h1.getRooms()) {
            for (Booking b : r.bookings) {
                System.out.println(" Room " + r.getRoomNumber() + ": " + b.getStatus() 
                    + " [" + b.getStartDate() + " to " + b.getEndDate() + "]");
            }
        }
    }
}
