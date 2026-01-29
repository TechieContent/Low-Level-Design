/*
 * For detailed explanations, system design breakdown, and the UML diagram,
 * check out my medium article at: https://medium.com/@techiecontent/day-27-lld-design-online-shopping-cart-java-code-7a5693e99b26?postPublishedType=repub
 *
 /
 
import java.util.*;

// -- Product --
class Product {
    private final String productId;
    private final String name;
    private final double price;
    private int stockQty;

    public Product(String productId, String name, double price, int stockQty) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stockQty = stockQty;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStockQty() { return stockQty; }

    public void reduceStock(int qty) {
        if(qty > stockQty) throw new IllegalArgumentException("Not enough stock!");
        stockQty -= qty;
    }
    public void increaseStock(int qty) { stockQty += qty; }
}

// -- CartItem --
class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getItemTotal() { return product.getPrice() * quantity; }
}

// -- Offer --
abstract class Offer {
    private final String offerId;
    private final String description;
    public Offer(String offerId, String description) {
        this.offerId = offerId; this.description = description;
    }
    public String getDescription() { return description; }
    public abstract double apply(Cart cart);
}

// Simple flat discount offer
class FlatDiscountOffer extends Offer {
    private final double discountAmount;
    public FlatDiscountOffer(String offerId, String desc, double discountAmount) {
        super(offerId, desc); this.discountAmount = discountAmount;
    }
    @Override
    public double apply(Cart cart) {
        return discountAmount; // Flat deduction from cart total
    }
}

// -- Cart --
class Cart {
    private final Map<String, CartItem> items = new HashMap<>(); // key: productId
    private final List<Offer> offers = new ArrayList<>();

    public void addItem(Product product, int qty) {
        if (product.getStockQty() < qty) {
            System.out.println("Cannot add to cart: Not enough stock for " + product.getName());
            return;
        }
        CartItem item = items.get(product.getProductId());
        if (item == null) {
            items.put(product.getProductId(), new CartItem(product, qty));
        } else {
            item.setQuantity(item.getQuantity() + qty);
        }
        System.out.println("Added " + qty + " of " + product.getName() + " to cart.");
    }

    public void removeItem(Product product) {
        items.remove(product.getProductId());
        System.out.println("Removed " + product.getName() + " from cart.");
    }

    public void updateQuantity(Product product, int qty) {
        CartItem item = items.get(product.getProductId());
        if (item == null) return;
        if (qty <= 0) items.remove(product.getProductId());
        else item.setQuantity(qty);
        System.out.println("Updated " + product.getName() + " quantity to " + qty + ".");
    }

    public void viewCart() {
        System.out.println("Your Cart:");
        if (items.isEmpty()) { System.out.println("- (empty)"); return; }
        for (CartItem item : items.values()) {
            System.out.println("- " + item.getProduct().getName() + " x" + item.getQuantity()
                    + " ($" + item.getProduct().getPrice() + " each) = $" + item.getItemTotal());
        }
        System.out.println("Cart Subtotal: $" + getSubtotal());
    }

    public void applyOffer(Offer offer) {
        offers.add(offer);
        System.out.println("Applied offer: " + offer.getDescription());
    }

    public double getSubtotal() {
        double sum = 0.0;
        for (CartItem item : items.values()) {
            sum += item.getItemTotal();
        }
        return sum;
    }

    public double getTotal() {
        double subtotal = getSubtotal();
        double totalDiscount = 0.0;
        for (Offer offer : offers) {
            totalDiscount += offer.apply(this);
        }
        double tax = 0.1 * subtotal; // example: 10% tax
        double finalTotal = subtotal + tax - totalDiscount;
        if (finalTotal < 0) finalTotal = 0;
        return finalTotal;
    }

    // On checkout: check stock, adjust inventory, return Order or null if fails
    public Order checkout(User user) {
        // First, stock check
        for (CartItem item : items.values()) {
            if (item.getProduct().getStockQty() < item.getQuantity()) {
                System.out.println("Cannot checkout: Not enough stock for " + item.getProduct().getName());
                return null;
            }
        }
        // All good, reduce stock
        for (CartItem item : items.values()) {
            item.getProduct().reduceStock(item.getQuantity());
        }
        Order order = new Order(UUID.randomUUID().toString(), user, new ArrayList<>(items.values()), getTotal());
        System.out.println("Checkout successful! Order Total: $" + getTotal());
        items.clear(); // Empty cart
        offers.clear();
        return order;
    }
}

// -- Order --
class Order {
    private final String orderId;
    private final User user;
    private final List<CartItem> orderedItems;
    private final double totalAmount;

    public Order(String orderId, User user, List<CartItem> orderedItems, double totalAmount) {
        this.orderId = orderId;
        this.user = user;
        this.orderedItems = orderedItems;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() { return orderId; }
    public User getUser() { return user; }
    public double getTotalAmount() { return totalAmount; }
}

// -- User --
class User {
    private final String userId;
    private final String name;
    private final Cart cart;

    public User(String userId, String name) {
        this.userId = userId; this.name = name;
        this.cart = new Cart();
    }

    public Cart getCart() { return cart; }
    public String getName() { return name; }
}

// -- Demo --
public class ShoppingCartDemo {
    public static void main(String[] args) {
        // Set up products
        Product apple = new Product("p1", "Apple", 1.0, 10);
        Product bread = new Product("p2", "Bread", 2.5, 5);
        Product cheese = new Product("p3", "Cheese", 3.99, 3);

        // Set up user
        User user = new User("u1", "Alice");

        // Simulate shopping
        Cart cart = user.getCart();
        cart.addItem(apple, 3);
        cart.addItem(bread, 2);
        cart.viewCart();

        cart.updateQuantity(apple, 5);
        cart.removeItem(bread);
        cart.viewCart();

        // Apply discount offer
        Offer offer = new FlatDiscountOffer("off1", "Save $2", 2.0);
        cart.applyOffer(offer);
        System.out.println("Final Cart Total (incl. 10% tax, minus offer): $" + cart.getTotal());

        // Checkout
        Order order = cart.checkout(user);
        if (order != null) {
            System.out.println("Order placed for " + user.getName() + ": $" + order.getTotalAmount() + " [Order #" + order.getOrderId() + "]");
        }
    }
}
