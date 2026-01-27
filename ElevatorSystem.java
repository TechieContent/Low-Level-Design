import java.util.*;

enum Direction { UP, DOWN, NONE }
enum DoorState { OPEN, CLOSED, MOVING }

// Request for an elevator from a floor or inside
class Request {
    int floor;
    Direction direction; // null if it’s just a floor choice inside elevator

    public Request(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }
}

// Door representation
class Door {
    DoorState state = DoorState.CLOSED;

    void open() { 
        state = DoorState.OPEN; 
        System.out.println("Door opened");
    }
    void close() { 
        state = DoorState.CLOSED;
        System.out.println("Door closed");
    }
    DoorState getState() {
        return state;
    }
}

// Elevator car
class Elevator {
    int elevatorId;
    int currentFloor = 0;
    Direction direction = Direction.NONE;
    TreeSet<Integer> destinations = new TreeSet<>();
    Door door = new Door();

    public Elevator(int id, int startingFloor) {
        elevatorId = id;
        currentFloor = startingFloor;
    }

    // Called when inside the elevator, or assigned a new request
    public void selectFloor(int floor) {
        destinations.add(floor);
        System.out.println("Elevator " + elevatorId + ": Added destination " + floor);
    }

    // Move one step closer to next destination; open/close door at floor
    public void step() {
        if (destinations.isEmpty()) {
            direction = Direction.NONE;
            return;
        }
        
        int nextFloor = direction == Direction.UP || direction == Direction.NONE
            ? destinations.first() : destinations.last();

        if (currentFloor == nextFloor) {
            // Arrived at a destination
            System.out.println("Elevator " + elevatorId + " at floor " + currentFloor + ", delivering...");
            door.open();
            destinations.remove(currentFloor);
            // Simulate wait then close the door immediately for this simple demo
            door.close();
            // Decide next direction
            direction = getDirectionForNext();
        } else if (currentFloor < nextFloor) {
            currentFloor++;
            direction = Direction.UP;
            System.out.println("Elevator " + elevatorId + " moving UP to floor " + currentFloor);
        } else if (currentFloor > nextFloor) {
            currentFloor--;
            direction = Direction.DOWN;
            System.out.println("Elevator " + elevatorId + " moving DOWN to floor " + currentFloor);
        }
    }

    private Direction getDirectionForNext() {
        if (destinations.isEmpty()) return Direction.NONE;
        int next = direction == Direction.DOWN ? destinations.last() : destinations.first();
        if (next > currentFloor) return Direction.UP;
        if (next < currentFloor) return Direction.DOWN;
        return Direction.NONE;
    }

    public boolean isIdle() {
        return destinations.isEmpty() && direction == Direction.NONE;
    }
    public int getCurrentFloor() { return currentFloor; }
    public String toString() {
        return "Elevator " + elevatorId + " at floor " + currentFloor + " going " + direction + ", door " + door.getState() + ", next stops " + destinations;
    }
}

// Floor representation
class Floor {
    int floorNumber;
    Button upButton, downButton;

    public Floor(int floorNumber, ElevatorSystem system) {
        this.floorNumber = floorNumber;
        upButton = new Button(this, Direction.UP, system);
        downButton = new Button(this, Direction.DOWN, system);
    }

    public void pressUp() { upButton.press(); }
    public void pressDown() { downButton.press(); }
}

// Button: floor/inside; tied to a floor and direction
class Button {
    Floor floor;
    Direction direction;
    ElevatorSystem system;

    public Button(Floor floor, Direction direction, ElevatorSystem system) {
        this.floor = floor;
        this.direction = direction;
        this.system = system;
    }
    public void press() {
        System.out.println("Button pressed: Floor " + floor.floorNumber + " " + direction);
        system.requestPickup(floor.floorNumber, direction);
    }
}

// Overall controller for entire building
class ElevatorSystem {
    List<Elevator> elevators = new ArrayList<>();
    List<Floor> floors = new ArrayList<>();
    Queue<Request> waitingRequests = new LinkedList<>();

    public ElevatorSystem(int numElevators, int numFloors) {
        for (int i = 0; i < numElevators; i++) {
            elevators.add(new Elevator(i, 0));
        }
        for (int i = 0; i < numFloors; i++) {
            floors.add(new Floor(i, this));
        }
    }

    // User presses up/down on a floor
    public void requestPickup(int floor, Direction dir) {
        System.out.println("System received pickup request at floor " + floor + " " + dir);
        Elevator best = findBestElevator(floor, dir);
        if (best != null) {
            best.selectFloor(floor);
            if (best.direction == Direction.NONE)
                best.direction = floor > best.currentFloor ? Direction.UP : Direction.DOWN;
        } else {
            System.out.println("All elevators busy. Queueing request.");
            waitingRequests.offer(new Request(floor, dir));
        }
    }

    // User inside elevator chooses a floor
    public void selectElevatorDestination(int elevatorId, int floor) {
        elevators.get(elevatorId).selectFloor(floor);
    }

    // Assign requests that were queued because all elevators were busy
    private void assignQueuedRequests() {
        Iterator<Request> it = waitingRequests.iterator();
        while (it.hasNext()) {
            Request req = it.next();
            Elevator best = findBestElevator(req.floor, req.direction);
            if (best != null) {
                best.selectFloor(req.floor);
                if (best.direction == Direction.NONE)
                    best.direction = req.floor > best.currentFloor ? Direction.UP : Direction.DOWN;
                it.remove();
            }
        }
    }

    // Simulate one step of all elevators
    public void step() {
        for (Elevator e : elevators) e.step();
        assignQueuedRequests();
    }

    // Find an idle or least-burdened elevator (simplified logic: first idle, else elevator with least destinations)
    private Elevator findBestElevator(int floor, Direction dir) {
        for (Elevator e : elevators) {
            if (e.isIdle()) return e;
        }
        // else, pick with fewest destinations (not very smart, just basic for demo)
        return elevators.stream().min(Comparator.comparingInt(e -> e.destinations.size())).orElse(null);
    }

    public void printStatus() {
        System.out.println("===== Elevator System Status =====");
        for (Elevator e : elevators) {
            System.out.println(e);
        }
        System.out.println("Queued requests: " + waitingRequests.size());
        System.out.println("==================================");
    }

    // For test/demo use: get floor object
    public Floor getFloor(int floorNumber) { return floors.get(floorNumber); }
}

public class ElevatorDemo {
    public static void main(String[] args) {
        ElevatorSystem system = new ElevatorSystem(2, 10);

        // Simulate floor requests
        system.getFloor(0).pressUp(); // Person at ground floor wants to go up
        system.getFloor(5).pressDown(); // Person at floor 5 wants to go down

        // Simulate inside elevator destination requests
        system.selectElevatorDestination(0, 8); // User inside elevator 0 selects floor 8
        
        // Simulate steps of the system
        for (int i = 0; i < 15; i++) {
            system.step();
            system.printStatus();
            try { Thread.sleep(500); } catch (Exception ignored) {}
        }
    }
}
