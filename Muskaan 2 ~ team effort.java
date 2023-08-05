import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String DB_NAME = "bank_accounts";
    private static final String COLLECTION_NAME = "accounts";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<BankAccount> accounts = new ArrayList<>();

        MongoDBManager dbManager = new MongoDBManager(DB_NAME, COLLECTION_NAME);

        // Load existing accounts from the database into the list
        accounts.addAll(dbManager.loadAccounts());

        char option;

        do {
            System.out.println("Choose an option: (a)Add Account (l)Display Accounts (s)Save to database (d) Deposit funds (w)Withdraw funds (q)Quit");
            option = scanner.next().charAt(0);
            scanner.nextLine(); // Consume the newline character left by next()

            switch (option) {
                case 'a':
                    addAccount(accounts, scanner);
                    break;
                case 'l':
                    displayAccounts(accounts);
                    break;
                case 's':
                    dbManager.saveAccounts(accounts);
                    System.out.println("Accounts saved to the database.");
                    break;
                case 'd':
                    depositFunds(accounts, scanner);
                    break;
                case 'w':
                    withdrawFunds(accounts, scanner);
                    break;
                case 'q':
                    dbManager.saveAccounts(accounts); // Save accounts before quitting
                    System.out.println("Quitting the program.");
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
                    break;
            }
        } while (option != 'q');

        dbManager.closeConnection();
        scanner.close();
    }

    private static void addAccount(List<BankAccount> accounts, Scanner scanner) {
        System.out.print("Choose the type of account (b: BankAccount, s: SavingsAccount): ");
        String accountType = scanner.nextLine();

        System.out.print("Enter account name: ");
        String accountName = scanner.nextLine();

        System.out.print("Enter account balance: ");
        double balance = scanner.nextDouble();
        scanner.nextLine(); // Consume the newline character left by nextDouble()

        BankAccount account;

        if (accountType.equalsIgnoreCase("s")) {
            account = new SavingsAccount(accountName, balance);
        } else {
            account = new BankAccount(accountName, balance);
        }

        accounts.add(account);
        System.out.println("Account added successfully!");
    }

    private static void displayAccounts(List<BankAccount> accounts) {
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
        } else {
            System.out.println("Accounts:");
            for (BankAccount account : accounts) {
                System.out.println("Account ID: " + account.getId());
                System.out.println("Account Name: " + account.getAccountName());
                System.out.println("Account Balance: " + account.getBalance());
                System.out.println("---------------");
            }
        }
    }

    private static void depositFunds(List<BankAccount> accounts, Scanner scanner) {
        System.out.print("Enter account ID to deposit funds: ");
        String accountId = scanner.nextLine();
        System.out.print("Enter amount to deposit: ");
        double amount = scanner.nextDouble();

        for (BankAccount account : accounts) {
            if (account.getId().equals(accountId)) {
                account.deposit(amount);
                System.out.println("Funds deposited successfully!");
                return;
            }
        }

        System.out.println("Account not found.");
    }

    private static void withdrawFunds(List<BankAccount> accounts, Scanner scanner) {
        System.out.print("Enter account ID to withdraw funds: ");
        String accountId = scanner.nextLine();
        System.out.print("Enter amount to withdraw: ");
        double amount = scanner.nextDouble();

        for (BankAccount account : accounts) {
            if (account.getId().equals(accountId)) {
                account.withdraw(amount);
                System.out.println("Funds withdrawn successfully!");
                return;
            }
        }

        System.out.println("Account not found.");
    }
}

class MongoDBManager {
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public MongoDBManager(String dbName, String collectionName) {
        database = MongoClients.create().getDatabase(dbName);
        collection = database.getCollection(collectionName);
    }

    public List<BankAccount> loadAccounts() {
        List<BankAccount> accounts = new ArrayList<>();

        for (Document document : collection.find()) {
            String id = document.get("_id").toString();
            String accountName = document.getString("accountName");
            double balance = document.getDouble("balance");

            BankAccount account = new BankAccount(accountName, balance);
            account.setId(id);
            accounts.add(account);
        }

        return accounts;
    }

    public void saveAccounts(List<BankAccount> accounts) {
        collection.drop(); // Clear the existing collection

        for (BankAccount account : accounts) {
            Document document = new Document("_id", account.getId())
                    .append("accountName", account.getAccountName())
                    .append("balance", account.getBalance());

            collection.insertOne(document);
        }
    }

    public void closeConnection() {
        MongoClients.create().close();
    }
}

class BankAccount {
    private String id;
    private String accountName;
    private double balance;

    public BankAccount(String accountName, double balance) {
        this.accountName = accountName;
        this.balance = balance;
        this.id = new ObjectId().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Other getters and setters as needed

    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) {
        balance -= amount;
    }
}

class SavingsAccount extends BankAccount {
    public SavingsAccount(String accountName, double balance) {
        super(accountName, balance);
    }

    @Override
    public void withdraw(double amount) {
        if (getBalance() - amount >= 100) {
            super.withdraw(amount);
        } else {
            System.out.println("Withdrawal not allowed. Minimum balance should be 100.");
        }
    }
}