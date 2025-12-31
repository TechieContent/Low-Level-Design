/*
 * For detailed explanations, system design breakdown, and the UML diagram,
 * check out my medium article at: https://medium.com/@techiecontent/day-10-design-a-splitwise-like-expense-sharing-app-af2336da0fdc?sk=e5d5484df1a921c512f89d69c3e4bdac
 *
/

import java.util.*;
import java.time.*;

// USER
class User {
    private final String userId;
    private final String name;
    private final String email;

    // For demo, groups not strictly needed but added for completeness
    private List<Group> groups = new ArrayList<>();

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }

    public void joinGroup(Group group) {
        if (!groups.contains(group)) groups.add(group);
    }
}

// GROUP & EXPENSE SYSTEM
class Group {
    private final String groupId;
    private final String name;
    private final List<User> members = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();
    private final BalanceSheet balanceSheet = new BalanceSheet();

    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    public String getGroupId() { return groupId; }
    public String getName() { return name; }
    public List<User> getMembers() { return members; }
    public BalanceSheet getBalanceSheet() { return balanceSheet; }

    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
            user.joinGroup(this);
        }
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        balanceSheet.updateBalances(expense);
    }

    public void recordPayment(Payment payment) {
        balanceSheet.applyPayment(payment);
    }

    public void printBalances() {
        System.out.println("Balance Sheet for group: " + name);
        balanceSheet.print();
    }
}

class Expense {
    private final String expenseId;
    private final String title;
    private final User paidBy;
    private final double amount;
    private final LocalDate date;
    private final List<Split> splits = new ArrayList<>();

    public Expense(String expenseId, String title, User paidBy, double amount, LocalDate date, List<Split> splits) {
        this.expenseId = expenseId;
        this.title = title;
        this.paidBy = paidBy;
        this.amount = amount;
        this.date = date;
        this.splits.addAll(splits);
    }

    public String getTitle() { return title; }
    public User getPaidBy() { return paidBy; }
    public double getAmount() { return amount; }
    public List<Split> getSplits() { return splits; }
}

class Split {
    private final User user;
    private final double amount;

    public Split(User user, double amount) {
        this.user = user;
        this.amount = amount;
    }
    public User getUser() { return user; }
    public double getAmount() { return amount; }
}

// BALANCE SHEET
class BalanceSheet {
    // user -> (owes user -> amount)
    private final Map<User, Map<User, Double>> groupBalances = new HashMap<>();

    public void updateBalances(Expense expense) {
        User paidBy = expense.getPaidBy();
        for (Split split : expense.getSplits()) {
            User user = split.getUser();
            double share = split.getAmount();
            if (user == paidBy) continue;
            put(user, paidBy, share); // user owes paidBy
        }
    }

    public void applyPayment(Payment payment) {
        put(payment.getFromUser(), payment.getToUser(), -payment.getAmount());
    }

    // Helper for adding debts/credits
    private void put(User from, User to, double val) {
        if (from == to) return;
        groupBalances.putIfAbsent(from, new HashMap<>());
        groupBalances.putIfAbsent(to, new HashMap<>());

        double prev = groupBalances.get(from).getOrDefault(to, 0.0);
        groupBalances.get(from).put(to, prev + val);

        // Mirror negative balance on other side for easy netting
        double prevReverse = groupBalances.get(to).getOrDefault(from, 0.0);
        groupBalances.get(to).put(from, prevReverse - val);
    }

    public void print() {
        for (User u1 : groupBalances.keySet()) {
            for (User u2 : groupBalances.get(u1).keySet()) {
                double amt = groupBalances.get(u1).get(u2);
                if (amt > 0.01) {
                    System.out.printf("%s owes %s: %.2f\n", u1.getName(), u2.getName(), amt);
                }
            }
        }
    }
}

// PAYMENT
class Payment {
    private final String paymentId;
    private final User fromUser;
    private final User toUser;
    private final double amount;
    private final LocalDateTime paymentTime;

    public Payment(String paymentId, User fromUser, User toUser, double amount, LocalDateTime paymentTime) {
        this.paymentId = paymentId;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.paymentTime = paymentTime;
    }

    public User getFromUser() { return fromUser; }
    public User getToUser() { return toUser; }
    public double getAmount() { return amount; }
}

// DEMO CLASS
public class SplitwiseDemo {
    public static void main(String[] args) {
        // Create some users
        User alice = new User("u1", "Alice", "alice@email.com");
        User bob = new User("u2", "Bob", "bob@email.com");
        User carol = new User("u3", "Carol", "carol@email.com");

        // Create group
        Group trip = new Group("g1", "Goa Trip");
        trip.addMember(alice);
        trip.addMember(bob);
        trip.addMember(carol);

        // Alice pays 300 for dinner, split equally
        List<Split> dinnerSplits = Arrays.asList(
            new Split(alice, 100),
            new Split(bob, 100),
            new Split(carol, 100));
        Expense dinner = new Expense("e1", "Dinner", alice, 300, LocalDate.now(), dinnerSplits);
        trip.addExpense(dinner);

        trip.printBalances();

        // Bob pays 150 for taxi, split equally
        List<Split> taxiSplits = Arrays.asList(
            new Split(alice, 50),
            new Split(bob, 50),
            new Split(carol, 50));
        Expense taxi = new Expense("e2", "Taxi", bob, 150, LocalDate.now(), taxiSplits);
        trip.addExpense(taxi);

        trip.printBalances();

        // Carol settles up with Alice for 100
        Payment payment = new Payment("p1", carol, alice, 100, LocalDateTime.now());
        trip.recordPayment(payment);

        trip.printBalances();
    }
}
