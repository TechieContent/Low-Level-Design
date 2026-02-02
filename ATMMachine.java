/*
 * For detailed explanations, system design breakdown, and the UML diagram,
 * check out my medium article at: https://medium.com/@techiecontent/e20d16df47b4?postPublishedType=initial
 *
/
import java.util.*;

// ----- Entities -----
class User {
    private String name;
    public User(String name) { this.name = name; }
    public String getName() { return name; }
}

class Card {
    private final String cardNumber;
    private final String pin;
    private final User user;

    public Card(String cardNumber, String pin, User user) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.user = user;
    }
    public String getCardNumber() { return cardNumber; }
    public String getPin() { return pin; }
    public User getUser() { return user; }
}

class Account {
    private final String accountNumber;
    private final User user;
    private double balance;
    private final List<Transaction> transactions = new ArrayList<>();
    private final double dailyLimit = 500.0;
    private double withdrawnToday = 0.0;

    public Account(String accountNumber, User user, double balance) {
        this.accountNumber = accountNumber;
        this.user = user;
        this.balance = balance;
    }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }

    public boolean canWithdraw(double amount) {
        return balance >= amount && (withdrawnToday + amount <= dailyLimit);
    }

    public boolean withdraw(double amount) {
        if (!canWithdraw(amount)) return false;
        balance -= amount;
        withdrawnToday += amount;
        transactions.add(new Transaction(new Date(), "WITHDRAW", amount));
        return true;
    }

    public void addTransaction(Transaction tx) {
        transactions.add(tx);
    }

    public List<Transaction> getTransactions() { return transactions; }
    public User getUser() { return user; }
}

class Transaction {
    private final Date date;
    private final String type;
    private final double amount;

    public Transaction(Date date, String type, double amount) {
        this.date = date;
        this.type = type;
        this.amount = amount;
    }

    public String toString() {
        return type + " $" + amount + " on " + date;
    }
}

class CashDispenser {
    private int cashAvailable;
    public CashDispenser(int init) { cashAvailable = init; }

    public boolean hasCash(double amount) { return cashAvailable >= (int) amount; }
    public boolean dispense(double amount) {
        if (hasCash(amount)) {
            cashAvailable -= (int) amount;
            System.out.println("Dispensed cash: $" + amount);
            return true;
        }
        System.out.println("ATM out of cash!");
        return false;
    }
    public int getCashAvailable() { return cashAvailable; }
}

// ----- Bank -----
class Bank {
    // For demo, simple maps
    private Map<String, Card> cards = new HashMap<>();
    private Map<String, Account> accounts = new HashMap<>();
    private Map<String, String> cardAccountMap = new HashMap<>(); // card number -> account number
    private Set<String> blockedCards = new HashSet<>();

    public void register(Card card, Account account) {
        cards.put(card.getCardNumber(), card);
        accounts.put(account.getAccountNumber(), account);
        cardAccountMap.put(card.getCardNumber(), account.getAccountNumber());
    }

    public boolean isBlocked(Card card) { return blockedCards.contains(card.getCardNumber()); }

    public boolean validate(Card card, String pin) {
        if (isBlocked(card)) return false;
        return card.getPin().equals(pin);
    }

    public Account getAccount(Card card) {
        String accNum = cardAccountMap.get(card.getCardNumber());
        return accounts.get(accNum);
    }

    public void blockCard(Card card) { blockedCards.add(card.getCardNumber()); }
}

// ----- ATM -----
class ATM {
    private final Bank bank;
    private final CashDispenser dispenser;
    private Card insertedCard;
    private Account currentAccount;
    private int pinAttempts = 0;
    private static final int MAX_PIN_ATTEMPTS = 3;

    public ATM(Bank bank, CashDispenser dispenser) {
        this.bank = bank; this.dispenser = dispenser;
    }

    public void insertCard(Card card) {
        if (bank.isBlocked(card)) {
            System.out.println("This card is blocked.");
            return;
        }
        insertedCard = card;
        pinAttempts = 0;
        System.out.println("Card inserted. Welcome, " + card.getUser().getName());
    }

    public boolean enterPIN(String pin) {
        if (insertedCard == null) { System.out.println("No card inserted."); return false; }
        if (bank.validate(insertedCard, pin)) {
            currentAccount = bank.getAccount(insertedCard);
            System.out.println("PIN correct.");
            return true;
        } else {
            pinAttempts++;
            if (pinAttempts >= MAX_PIN_ATTEMPTS) {
                bank.blockCard(insertedCard);
                System.out.println("Card blocked due to too many wrong PIN attempts.");
                ejectCard();
                return false;
            }
            System.out.println("Wrong PIN. Attempts left: " + (MAX_PIN_ATTEMPTS - pinAttempts));
            return false;
        }
    }

    public void showMenu() {
        System.out.println("\n--- ATM Menu ---");
        System.out.println("1. Check Balance");
        System.out.println("2. Withdraw Cash");
        System.out.println("3. Recent Transactions");
        System.out.println("4. Exit");
    }

    public void checkBalance() {
        if (currentAccount != null)
            System.out.println("Your balance: $" + currentAccount.getBalance());
    }

    public void withdraw(double amount) {
        if (currentAccount == null) return;
        if (!currentAccount.canWithdraw(amount)) {
            System.out.println("Withdrawal denied: insufficient balance or daily limit exceeded.");
            return;
        }
        if (!dispenser.hasCash(amount)) {
            System.out.println("ATM does not have enough cash.");
            return;
        }
        if (currentAccount.withdraw(amount) && dispenser.dispense(amount)) {
            System.out.println("Withdrawal successful. Balance now $" + currentAccount.getBalance());
        }
    }

    public void showRecentTransactions() {
        if (currentAccount != null) {
            System.out.println("Recent Transactions:");
            List<Transaction> txs = currentAccount.getTransactions();
            if (txs.isEmpty()) System.out.println("(none)");
            for (Transaction t : txs)
                System.out.println(t);
        }
    }

    public void ejectCard() {
        insertedCard = null;
        currentAccount = null;
        pinAttempts = 0;
        System.out.println("Card ejected. Thank you!");
    }
}

// ----- Demo -----
public class ATMDemo {
    public static void main(String[] args) {
        // Setup
        User alice = new User("Alice");
        Card card = new Card("1111-2222-3333-4444", "1234", alice);
        Account account = new Account("AC1001", alice, 800.0);
        Bank bank = new Bank();
        bank.register(card, account);

        ATM atm = new ATM(bank, new CashDispenser(1000)); // $1000 in ATM

        // Simulate a session
        atm.insertCard(card);

        Scanner in = new Scanner(System.in);
        int steps = 0;
        boolean authed = false;
        while (!authed && steps < 5) {
            System.out.print("Enter PIN: ");
            String p = in.nextLine();
            authed = atm.enterPIN(p);
            steps++;
            if (bank.isBlocked(card)) return;
        }

        boolean session = authed;
        while (session) {
            atm.showMenu();
            System.out.print("Choose option: ");
            int opt = in.nextInt();
            switch (opt) {
                case 1: atm.checkBalance(); break;
                case 2:
                    System.out.print("Withdraw amount: $");
                    double amount = in.nextDouble();
                    atm.withdraw(amount);
                    break;
                case 3: atm.showRecentTransactions(); break;
                case 4: 
                    atm.ejectCard();
                    session = false;
                    break;
                default: System.out.println("Invalid option.");
            }
        }
    }
}
