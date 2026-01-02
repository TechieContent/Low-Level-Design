/*
 * For detailed explanations, system design breakdown, and the UML diagram,
 * check out my medium article at: https://medium.com/@techiecontent/day-11-design-a-ride-sharing-app-like-uber-704e9e4f6cf4?sk=34e1b856b4c094511d503011c4789c5c
 *
/

import java.util.*;
import java.time.*;

// ENUMS
enum RideRequestStatus { REQUESTED, MATCHED, CANCELLED }
enum TripStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED }
enum PaymentStatus { PENDING, SUCCESS, FAILED }

// USER BASE + ROLES
class User {
    protected String userId;
    protected String name;
    protected String phone;

    public User(String userId, String name, String phone) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
}

class Rider extends User {
    private List<Trip> tripHistory = new ArrayList<>();

    public Rider(String userId, String name, String phone) {
        super(userId, name, phone);
    }

    public RideRequest requestRide(Location pickup, Location drop) {
        System.out.println(name + " requested a ride from " + pickup + " to " + drop);
        return new RideRequest(UUID.randomUUID().toString(), this, pickup, drop, RideRequestStatus.REQUESTED);
    }

    public void addTrip(Trip trip) {
        tripHistory.add(trip);
    }

    public void viewTripHistory() {
        System.out.println("Trip History for " + name + ":");
        for (Trip t : tripHistory) {
            System.out.println("  " + t);
        }
    }
}

class Driver extends User {
    private Vehicle vehicle;
    private Location currentLocation;
    private boolean available;
    private List<Trip> trips = new ArrayList<>();

    public Driver(String userId, String name, String phone, Vehicle vehicle, Location startingLoc) {
        super(userId, name, phone);
        this.vehicle = vehicle;
        this.currentLocation = startingLoc;
        this.available = true;
    }

    public void updateLocation(Location loc) {
        this.currentLocation = loc;
    }

    public Location getCurrentLocation() { return currentLocation; }

    public Vehicle getVehicle() { return vehicle; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean avail) { this.available = avail; }

    public Trip acceptRide(RideRequest rideRequest) {
        if (!available || rideRequest.getStatus() != RideRequestStatus.REQUESTED) {
            System.out.println(name + " is not available to accept ride.");
            return null;
        }
        rideRequest.setStatus(RideRequestStatus.MATCHED);
        this.available = false;
        Trip trip = new Trip(UUID.randomUUID().toString(), rideRequest, rideRequest.getRider(), this, rideRequest.getPickup(), rideRequest.getDrop());
        trips.add(trip);
        rideRequest.getRider().addTrip(trip);
        System.out.println(name + " accepted the ride request.");
        return trip;
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }
}

// VEHICLE & LOCATION
class Vehicle {
    private String vehicleId;
    private String model;
    private String licensePlate;

    public Vehicle(String vehicleId, String model, String licensePlate) {
        this.vehicleId = vehicleId;
        this.model = model;
        this.licensePlate = licensePlate;
    }

    public String toString() {
        return model + " (" + licensePlate + ")";
    }
}

class Location {
    private double latitude;
    private double longitude;

    public Location(double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public String toString() {
        return String.format("(%.3f, %.3f)", latitude, longitude);
    }

    // For "distance" estimation (Pythagorean for demo)
    public double distanceTo(Location other) {
        double dLat = this.latitude - other.latitude;
        double dLon = this.longitude - other.longitude;
        return Math.sqrt(dLat*dLat + dLon*dLon);
    }
}

// RIDE REQUEST, TRIP, PAYMENT
class RideRequest {
    private String requestId;
    private Rider rider;
    private Location pickup;
    private Location drop;
    private RideRequestStatus status;

    public RideRequest(String requestId, Rider rider, Location pickup, Location drop, RideRequestStatus status) {
        this.requestId = requestId;
        this.rider = rider;
        this.pickup = pickup;
        this.drop = drop;
        this.status = status;
    }

    public String getRequestId() { return requestId; }
    public Rider getRider() { return rider; }
    public Location getPickup() { return pickup; }
    public Location getDrop() { return drop; }
    public RideRequestStatus getStatus() { return status; }
    public void setStatus(RideRequestStatus s) { this.status = s; }
}

class Trip {
    private String tripId;
    private RideRequest rideRequest;
    private Rider rider;
    private Driver driver;
    private Location pickup;
    private Location drop;
    private TripStatus status;
    private double fare;
    private Payment payment;
    private LocalDateTime startTime, endTime;

    public Trip(String tripId, RideRequest rideRequest, Rider rider, Driver driver, Location pickup, Location drop) {
        this.tripId = tripId;
        this.rideRequest = rideRequest;
        this.rider = rider;
        this.driver = driver;
        this.pickup = pickup;
        this.drop = drop;
        this.status = TripStatus.NOT_STARTED;
        this.fare = 0.0;
    }

    public void startTrip() {
        if (status == TripStatus.NOT_STARTED) {
            status = TripStatus.IN_PROGRESS;
            startTime = LocalDateTime.now();
            System.out.println("Trip started.");
        }
    }

    public void endTrip() {
        if (status == TripStatus.IN_PROGRESS) {
            status = TripStatus.COMPLETED;
            endTime = LocalDateTime.now();
            fare = estimateFare();
            // Create payment (for demo, always successful)
            payment = new Payment(UUID.randomUUID().toString(), PaymentStatus.SUCCESS, fare);
            driver.setAvailable(true);
            System.out.printf("Trip completed. Fare: %.2f\n", fare);
        }
    }

    private double estimateFare() {
        // Fare = 50 (base fare) + 10 * distance
        double dist = pickup.distanceTo(drop);
        return 50 + 10 * dist;
    }

    public String toString() {
        return String.format("Trip[%s]: %s to %s, Rider=%s, Driver=%s, Status=%s, Fare=%.2f",
                tripId, pickup, drop, rider.getName(), driver.getName(), status, fare);
    }
}

class Payment {
    private String paymentId;
    private PaymentStatus status;
    private double amount;

    public Payment(String paymentId, PaymentStatus status, double amount) {
        this.paymentId = paymentId;
        this.status = status;
        this.amount = amount;
    }
}

// MATCHING LOGIC
class RideMatchingService {
    public static Driver findNearestAvailableDriver(List<Driver> drivers, Location pickup) {
        double minDist = Double.MAX_VALUE;
        Driver selected = null;
        for (Driver d : drivers) {
            if (d.isAvailable()) {
                double dist = d.getCurrentLocation().distanceTo(pickup);
                if (dist < minDist) {
                    minDist = dist;
                    selected = d;
                }
            }
        }
        return selected;
    }
}

// DEMO SIMULATION
public class RideSharingDemo {
    public static void main(String[] args) {
        // Setup drivers and riders
        Vehicle v1 = new Vehicle("veh1", "Toyota Prius", "ABC123");
        Vehicle v2 = new Vehicle("veh2", "Honda City", "XYZ999");
        Driver driver1 = new Driver("d1", "Bob", "99999", v1, new Location(12.950, 77.620));
        Driver driver2 = new Driver("d2", "Anita", "77777", v2, new Location(12.900, 77.600));

        Rider rider = new Rider("r1", "Alice", "88888");

        List<Driver> allDrivers = Arrays.asList(driver1, driver2);

        // Alice requests a ride from point A to B
        Location pickup = new Location(12.960, 77.625);
        Location drop = new Location(12.990, 77.700);

        RideRequest rideReq = rider.requestRide(pickup, drop);

        // Matching logic - find nearest available driver and assign
        Driver nearest = RideMatchingService.findNearestAvailableDriver(allDrivers, pickup);
        if (nearest == null) {
            System.out.println("No available drivers found.");
            return;
        }
        Trip trip = nearest.acceptRide(rideReq);
        if (trip == null) {
            System.out.println("Driver couldn't accept ride.");
            return;
        }

        // Start and end trip
        trip.startTrip();
        // (simulate trip duration)
        trip.endTrip();

        // Show trip histories
        rider.viewTripHistory();
        System.out.println("Driver's recent trip: " + trip);
    }
}
