/*
 * For detailed explanations, system design breakdown, and the UML diagram,
 * check out my medium article at: https://medium.com/@techiecontent/day-7-design-a-parking-lot-system-cb9f71b0fb23?sk=be6074a896caf865adb5c07d913199d4
 *
/
 
import java.util.*;
import java.time.*;

enum SpotType { REGULAR, COMPACT, LARGE, HANDICAPPED }
enum VehicleType { CAR, MOTORCYCLE, BUS }

class Vehicle {
    private String licenseNumber;
    private VehicleType type;

    public Vehicle(String licenseNumber, VehicleType type) {
        this.licenseNumber = licenseNumber;
        this.type = type;
    }
    public VehicleType getType() { return type; }
    public String getLicenseNumber() { return licenseNumber; }
}

class ParkingSpot {
    private String spotNumber;
    private boolean isOccupied;
    private SpotType type;
    private Vehicle parkedVehicle;

    public ParkingSpot(String spotNumber, SpotType type) {
        this.spotNumber = spotNumber;
        this.type = type;
        this.isOccupied = false;
    }

    public boolean isAvailable() { return !isOccupied; }
    public SpotType getType() { return type; }
    public String getSpotNumber() { return spotNumber; }

    public boolean canFit(Vehicle vehicle) {
        // For demo, assume car/motorcycle → REGULAR/COMPACT, bus → LARGE
        switch (vehicle.getType()) {
            case MOTORCYCLE: return (type == SpotType.COMPACT || type == SpotType.REGULAR);
            case CAR: return (type == SpotType.REGULAR || type == SpotType.LARGE);
            case BUS: return (type == SpotType.LARGE);
            default: return false;
        }
    }

    public boolean park(Vehicle vehicle) {
        if (!isAvailable() || !canFit(vehicle)) return false;
        this.parkedVehicle = vehicle;
        this.isOccupied = true;
        return true;
    }

    public void free() {
        parkedVehicle = null;
        isOccupied = false;
    }
}

class ParkingLot {
    private List<ParkingFloor> floors;

    public ParkingLot(int numFloors, Map<SpotType, Integer> spotsPerType) {
        this.floors = new ArrayList<>();
        for (int i = 0; i < numFloors; i++)
            floors.add(new ParkingFloor("F" + (i+1), spotsPerType));
    }

    // Find the first available spot for the vehicle
    public ParkingSpot findAvailableSpot(Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.getAvailableSpot(vehicle);
            if (spot != null) return spot;
        }
        return null;
    }

    public boolean isFull(Vehicle vehicle) {
        return findAvailableSpot(vehicle) == null;
    }

    // For demo
    public void displayStatus() {
        for (ParkingFloor floor : floors) {
            floor.display();
        }
    }
}

class ParkingFloor {
    private String floorId;
    private Map<SpotType, List<ParkingSpot>> spots;

    public ParkingFloor(String floorId, Map<SpotType, Integer> spotsPerType) {
        this.floorId = floorId;
        spots = new HashMap<>();
        for (SpotType t : SpotType.values()) {
            spots.put(t, new ArrayList<>());
            int count = spotsPerType.getOrDefault(t, 0);
            for (int i = 0; i < count; i++)
                spots.get(t).add(new ParkingSpot(floorId + "-" + t + "-" + (i+1), t));
        }
    }

    public ParkingSpot getAvailableSpot(Vehicle vehicle) {
        for (SpotType t : SpotType.values()) {
            for (ParkingSpot spot : spots.get(t)) {
                if (spot.isAvailable() && spot.canFit(vehicle))
                    return spot;
            }
        }
        return null;
    }

    public void display() {
        System.out.println("Floor: " + floorId);
        for (SpotType type : SpotType.values()) {
            long free = spots.get(type).stream().filter(ParkingSpot::isAvailable).count();
            System.out.println("  " + type + ": " + free + " available");
        }
    }
}

class ParkingTicket {
    private static int counter = 1;
    private final String ticketNumber;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketNumber = "T" + counter++;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = LocalDateTime.now();
    }

    public void setExitTime() { this.exitTime = LocalDateTime.now(); }
    public long getHoursParked() {
        if (exitTime == null) return 0;
        return Duration.between(entryTime, exitTime).toHours() + 1; // Always round up
    }

    public ParkingSpot getSpot() { return spot; }
    public Vehicle getVehicle() { return vehicle; }
    public String getTicketNumber() { return ticketNumber; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
}

class PaymentService {
    public static double calculateFee(VehicleType type, long hours) {
        // Simplified static rate per hour
        switch (type) {
            case CAR: return 20 * hours;
            case MOTORCYCLE: return 10 * hours;
            case BUS: return 50 * hours;
            default: return 0;
        }
    }
}

class ParkingSystem {
    private ParkingLot lot;
    // For simplicity, map ticket number to ticket
    private Map<String, ParkingTicket> activeTickets = new HashMap<>();

    public ParkingSystem(ParkingLot lot) {
        this.lot = lot;
    }

    public String enterParking(String licenseNumber, VehicleType type) {
        Vehicle vehicle = new Vehicle(licenseNumber, type);
        if (lot.isFull(vehicle)) {
            System.out.println("No spots available for this vehicle.");
            return null;
        }
        ParkingSpot spot = lot.findAvailableSpot(vehicle);
        spot.park(vehicle);
        ParkingTicket ticket = new ParkingTicket(vehicle, spot);
        activeTickets.put(ticket.getTicketNumber(), ticket);
        System.out.println("Vehicle parked. Ticket: " + ticket.getTicketNumber() + ", Spot: " + spot.getSpotNumber());
        return ticket.getTicketNumber();
    }

    public void exitParking(String ticketNumber) {
        ParkingTicket ticket = activeTickets.get(ticketNumber);
        if (ticket == null) {
            System.out.println("Invalid ticket number.");
            return;
        }
        ticket.setExitTime();
        long hours = ticket.getHoursParked();
        double fee = PaymentService.calculateFee(ticket.getVehicle().getType(), hours);
        ticket.getSpot().free();
        activeTickets.remove(ticketNumber);
        System.out.printf(
            "Vehicle %s exited. Hours: %d, Fee: %.2f\n",
            ticket.getVehicle().getLicenseNumber(), hours, fee
        );
    }

    public void displayLotStatus() {
        lot.displayStatus();
    }
}

public class ParkingLotDemo {
    public static void main(String[] args) {
        // Initialize lot with 2 floors, each floor with 2 compact, 2 regular, 1 large spots
        Map<SpotType, Integer> config = new HashMap<>();
        config.put(SpotType.COMPACT, 2);
        config.put(SpotType.REGULAR, 2);
        config.put(SpotType.LARGE, 1);

        ParkingLot lot = new ParkingLot(2, config);
        ParkingSystem system = new ParkingSystem(lot);

        system.displayLotStatus();
        String ticket1 = system.enterParking("AB1234", VehicleType.CAR);
        String ticket2 = system.enterParking("XY5678", VehicleType.MOTORCYCLE);
        String ticket3 = system.enterParking("BUS111", VehicleType.BUS);

        system.displayLotStatus();

        // Simulate vehicle exit
        system.exitParking(ticket2);
        system.exitParking(ticket1);

        system.displayLotStatus();
    }
}
