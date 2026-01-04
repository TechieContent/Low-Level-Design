import java.util.*;
import java.time.*;

// ENUMS
enum OrderStatus { PLACED, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }
enum PaymentStatus { PENDING, SUCCESS, FAILED }
enum DeliveryStatus { ASSIGNED, PICKED_UP, DELIVERED, CANCELLED }

// BASE USER & ROLES
class User {
    protected String userId, name, email, phone;
    public User(String userId, String name, String email, String phone) {
        this.userId = userId; this.name = name; this.email = email; this.phone = phone;
    }
    public String getName() { return name; }
}

class Customer extends User {
    private Cart cart = new Cart();
    private List<Order> orders = new ArrayList<>();

    public Customer(String userId, String name, String email, String phone) {
        super(userId, name, email, phone);
    }

    public List<Restaurant> viewRestaurants(List<Restaurant> allRestaurants) {
        return allRestaurants; // Filtering can be added later
    }

    public Cart getCart() { return cart; }

    public void placeOrder(Restaurant restaurant, Address deliveryAddress) {
        if (cart.isEmpty()) {
            System.out.println(name + "'s cart is empty.");
            return;
        }
        Order order = new Order(
            UUID.randomUUID().toString(), this, restaurant, 
            new ArrayList<>(cart.getItems()), OrderStatus.PLACED, 
            deliveryAddress
        );
        Payment payment = new Payment(UUID.randomUUID().toString(), PaymentStatus.SUCCESS, order.calculateTotalAmount(), LocalDateTime.now());
        order.setPayment(payment);
        restaurant.addOrder(order);
        orders.add(order);
        cart.clear();
        System.out.println(name + " placed an order. OrderID: " + order.getOrderId() + " Amount: " + order.calculateTotalAmount());
    }
}

class DeliveryAgent extends User {
    private Vehicle vehicle;
    private boolean available = true;

    public DeliveryAgent(String userId, String name, String email, String phone, Vehicle vehicle) {
        super(userId, name, email, phone);
        this.vehicle = vehicle;
    }
    public boolean isAvailable() { return available; }
    public void assignDelivery(Delivery delivery) { 
        this.available = false; 
        delivery.setAgent(this);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        System.out.println(name + " assigned delivery " + delivery.getDeliveryId());
    }
    public void completeDelivery(Delivery delivery) {
        delivery.setStatus(DeliveryStatus.DELIVERED);
        this.available = true;
        System.out.println(name + " delivered order " + delivery.getOrder().getOrderId());
    }
}

class RestaurantOwner extends User {
    public RestaurantOwner(String userId, String name, String email, String phone) {
        super(userId, name, email, phone);
    }
    public void addMenuItem(Restaurant restaurant, MenuItem item) {
        restaurant.addMenuItem(item);
    }
    public void updateMenu(Restaurant restaurant, MenuItem item) {
        // (Demo: just add)
        restaurant.addMenuItem(item);
    }
}

// RESTAURANT & MENU
class Restaurant {
    private String restaurantId, name;
    private Address address;
    private List<MenuItem> menu = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private RestaurantOwner owner;

    public Restaurant(String restaurantId, String name, Address address, RestaurantOwner owner) {
        this.restaurantId = restaurantId; this.name = name; this.address = address; this.owner = owner;
    }
    public List<MenuItem> getMenu() { return menu; }
    public void addMenuItem(MenuItem item) { menu.add(item); }
    public String getName() { return name; }
    public Address getAddress() { return address; }
    public void addOrder(Order order) { orders.add(order); }
}

class MenuItem {
    private String menuItemId, name, description;
    private double price;

    public MenuItem(String menuItemId, String name, String description, double price) {
        this.menuItemId = menuItemId; this.name = name; this.description = description; this.price = price;
    }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String toString() { return name + " ($" + price + ")"; }
}

// ADDRESS
class Address {
    private String street, city, zip, country;
    public Address(String street, String city, String zip, String country) {
        this.street = street; this.city = city; this.zip = zip; this.country = country;
    }
    public String toString() { return street + ", " + city + ", " + zip + ", " + country; }
}

// CART
class Cart {
    private List<MenuItem> items = new ArrayList<>();
    public void addItem(MenuItem item) { items.add(item); }
    public void removeItem(MenuItem item) { items.remove(item); }
    public void clear() { items.clear(); }
    public boolean isEmpty() { return items.isEmpty(); }
    public List<MenuItem> getItems() { return items; }
    public void printCart() {
        System.out.println("Cart items:");
        for(MenuItem item : items) System.out.println("- " + item);
    }
}

// ORDER, PAYMENT, DELIVERY
class Order {
    private String orderId;
    private Customer customer;
    private Restaurant restaurant;
    private List<MenuItem> items;
    private OrderStatus status;
    private Payment payment;
    private Address deliveryAddress;
    private Delivery delivery;

    public Order(String orderId, Customer customer, Restaurant restaurant, List<MenuItem> items, OrderStatus status, Address deliveryAddress) {
        this.orderId = orderId;
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = items;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
    }
    public String getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public Restaurant getRestaurant() { return restaurant; }
    public List<MenuItem> getItems() { return items; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public double calculateTotalAmount() {
        double sum = 0;
        for (MenuItem item : items) sum += item.getPrice();
        return sum;
    }
    public void setDelivery(Delivery delivery) { this.delivery = delivery; }
    public void setStatus(OrderStatus s) { status = s; }
    public Address getDeliveryAddress() { return deliveryAddress; }
    public String toString() {
        return "Order " + orderId + " (" + status + "), " + restaurant.getName() + " [" + items.size() + " items] to " + customer.getName();
    }
    public Payment getPayment() { return payment; }
    public Delivery getDelivery() { return delivery; }
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
}

class Delivery {
    private String deliveryId;
    private DeliveryAgent agent;
    private Order order;
    private DeliveryStatus status;
    public Delivery(String deliveryId, Order order) {
        this.deliveryId = deliveryId;
        this.order = order;
        this.status = DeliveryStatus.ASSIGNED;
    }
    public String getDeliveryId() { return deliveryId; }
    public Order getOrder() { return order; }
    public void setAgent(DeliveryAgent agent) { this.agent = agent; }
    public void setStatus(DeliveryStatus s) { status = s; }
    public DeliveryStatus getStatus() { return status; }
    public DeliveryAgent getAgent() { return agent; }
}

// VEHICLE
class Vehicle {
    private String model, plateNumber;
    public Vehicle(String model, String plateNumber) { this.model = model; this.plateNumber = plateNumber; }
    public String toString() { return model + " (" + plateNumber + ")"; }
}

// MAIN for DEMO
public class FoodOrderingDemo {
    public static void main(String[] args) {
        // Setup demo data: Owners, Restaurants, Menus, Agents, Customers
        RestaurantOwner owner = new RestaurantOwner("ow1", "Chef Mike", "mike@email.com", "99999");
        Restaurant r1 = new Restaurant("rest1", "Pizza Place", new Address("123 Main St", "Metro", "10001", "Country"), owner);
        owner.addMenuItem(r1, new MenuItem("mi1", "Margherita", "Cheese Pizza", 8.99));
        owner.addMenuItem(r1, new MenuItem("mi2", "Pepperoni", "Pepperoni Pizza", 10.99));
        owner.addMenuItem(r1, new MenuItem("mi3", "Veggie", "Vegetable Pizza", 9.49));

        Restaurant r2 = new Restaurant("rest2", "Taco Town", new Address("456 Oak Rd", "Metro", "10001", "Country"), owner);
        owner.addMenuItem(r2, new MenuItem("mi4", "Beef Taco", "Beef with salsa", 4.99));
        owner.addMenuItem(r2, new MenuItem("mi5", "Veg Taco", "Veggies with beans", 3.99));

        List<Restaurant> allRestaurants = Arrays.asList(r1, r2);

        DeliveryAgent agent1 = new DeliveryAgent("ag1", "Sandy", "sandy@email.com", "88888", new Vehicle("Bike", "B-123"));
        DeliveryAgent agent2 = new DeliveryAgent("ag2", "Tom", "tom@email.com", "77777", new Vehicle("Scooter", "S-456"));

        // Customer session
        Customer alice = new Customer("cu1", "Alice", "alice@email.com", "11111");

        System.out.println("\nRestaurants available:");
        for (Restaurant rest : alice.viewRestaurants(allRestaurants))
            System.out.println("- " + rest.getName() + ", " + rest.getAddress());

        // Alice views r1 menu
        System.out.println("\n" + alice.getName() + " is viewing " + r1.getName() + " menu:");
        for (MenuItem item : r1.getMenu())
            System.out.println("* " + item);

        // Alice adds items to cart and places an order
        alice.getCart().addItem(r1.getMenu().get(0)); // Margherita
        alice.getCart().addItem(r1.getMenu().get(2)); // Veggie
        alice.getCart().printCart();

        Address aliceAddress = new Address("100 Street", "Metro", "10001", "Country");
        alice.placeOrder(r1, aliceAddress);

        // Assign delivery agent (simplified matching for demo)
        Order placedOrder = r1.getMenu().size() > 0 ? r1.orders.get(0) : null;
        if (placedOrder != null) {
            Delivery delivery = new Delivery(UUID.randomUUID().toString(), placedOrder);
            placedOrder.setDelivery(delivery);
            if (agent1.isAvailable()) {
                agent1.assignDelivery(delivery);
            } else if (agent2.isAvailable()) {
                agent2.assignDelivery(delivery);
            }
            // Simulate delivery completion
            agent1.completeDelivery(delivery);
        }
    }
}
