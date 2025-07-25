package ABC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

    private static BillingService billingService = new BillingService();
    private static Scanner sc = new Scanner(System.in);
    private static User loggedInUser;
    
    

    public static void main(String[] args) {
        System.out.println("\n🍎🍌🍇 Welcome to the Fruit Billing System 🍉🍍🍓");
        System.out.println("==============================================");
        loginScreen();

        if (loggedInUser == null) {
            System.out.println("❌ Invalid login. Exiting...");
            System.exit(0);
        }
        System.out.println("✅ Login successful! Role: " + loggedInUser.getRole());
        if ("Admin".equalsIgnoreCase(loggedInUser.getRole())) {
            adminMenu();
        } else {
            billerMenu();
        }
        System.out.println("🌴 Thank you for using the Fruit Billing System! Goodbye! 🍍🍋");
        sc.close();
    }

    private static void loginScreen() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();

        System.out.print("Password: ");
        String password = sc.nextLine().trim();
        
        
        

        //System.out.println("Username entered: " + username);
       // System.out.println("Password entered: " + password);

        String hashedPassword = PasswordUtils.hashPassword(password);
        
        
        //System.out.println("Hashed password: " + hashedPassword);

        loggedInUser = billingService.login(username, hashedPassword); //checks with db
        
        
        
    }

    private static void adminMenu() {
        int choice;
        do {
            System.out.println("\n🌟 Admin Menu 🌟");
            System.out.println("1️🌟 Add or Refill Product Stock 📦");
            System.out.println("2️🌟 Change Product Price 💰");
            System.out.println("3️🌟 Show All Products 🍉");
            System.out.println("4️🌟 Show All Bills 🧾");
            System.out.println("5️🌟 Logout 🚪");
            System.out.print("Enter your choice (1-5): ");

            choice = getIntInput(1, 5);
            

            switch (choice) {
                case 1 -> addOrRefillProduct();
                case 2 -> changeProductPrice();
                case 3 -> showAllProducts();
                case 4 -> showAllBills();
                case 5 -> System.out.println("Logging out...");
            }
        } while (choice != 5);
    }

    private static void billerMenu() {
        int choice;
        do {
        	
        	
            System.out.println("\n🌟 Biller Menu 🌟");
            System.out.println("1️⃣ Create New Bill 🧾");
            System.out.println("2️⃣ Show All Products 🍉");
            System.out.println("3️⃣ Show All Bills 🧾");
            System.out.println("4️⃣ Logout 🚪");
            System.out.print("Enter your choice (1-4): ");

            choice = getIntInput(1, 4);
            
            

            switch (choice) {
                case 1 -> createNewBill();
                case 2 -> showAllProducts();
                case 3 -> showAllBills();
                case 4 -> System.out.println("Logging out...");
            }
        } while (choice != 4);
    }

    private static void addOrRefillProduct() {
        System.out.println("\n📦 Products List:");
        List<Product> products = billingService.getAllProducts();
        for (Product p : products) {
            System.out.printf("%d. %s (Category: %s, Price: ₹%.2f, Stock: %d)%n", p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock());
        }
        System.out.print("Enter product ID to add/refill stock or 0 to add a new product: ");
        int id = getIntInput(0, Integer.MAX_VALUE);
        if (id == 0) { //add
            System.out.print("Enter new product name: ");
            String name = sc.nextLine();

            System.out.print("Enter product category: ");
            String category = sc.nextLine();

            System.out.print("Enter price per unit (₹): ");
            double price = getDoubleInput(0.01, Double.MAX_VALUE);

            System.out.print("Enter stock quantity: ");
            int stock = getIntInput(0, Integer.MAX_VALUE);

            boolean success = insertNewProduct(name, category, price, stock);
            System.out.println(success ? "✅ New product added successfully!" : "❌ Failed to add product.");
        } else {
            // Refill stock 
            Product p = billingService.getProductById(id);
            if (p == null) {
                System.out.println("❌ Product not found.");
                return;
            }
            System.out.printf("Current stock of %s is %d%n", p.getName(), p.getStock());
            System.out.print("Enter quantity to add: ");
            int qty = getIntInput(1, Integer.MAX_VALUE);
            boolean success = billingService.updateProductStock(id, p.getStock() + qty);
            System.out.println(success ? "✅ Stock updated successfully!" : "❌ Failed to update stock.");
        }
    }

    private static boolean insertNewProduct(String name, String category, double price, int stock) {
        String sql = "INSERT INTO products (name, category, price, stock) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(BillingService.DB_URL, BillingService.USER, BillingService.PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setDouble(3, price);
            stmt.setInt(4, stock);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void changeProductPrice() {
        System.out.println("\n💰 Products List:");
        List<Product> products = billingService.getAllProducts();
        for (Product p : products) {
            System.out.printf("%d. %s (Price: ₹%.2f)%n", p.getId(), p.getName(), p.getPrice());
        }
        System.out.print("Enter product ID to change price: ");
        int id = getIntInput(1, Integer.MAX_VALUE);

        Product p = billingService.getProductById(id);
        if (p == null) {
            System.out.println("❌ Product not found.");
            return;
        }
        System.out.printf("Current price of %s is ₹%.2f%n", p.getName(), p.getPrice());
        System.out.print("Enter new price (₹): ");
        double newPrice = getDoubleInput(0.01, Double.MAX_VALUE);
        boolean success = billingService.updateProductPrice(id, newPrice);
        System.out.println(success ? "✅ Price updated successfully!" : "❌ Failed to update price.");
    }

    private static void showAllProducts() {
        System.out.println("\n🍉 Products List:");
        List<Product> products = billingService.getAllProducts();
        System.out.printf("%-5s %-20s %-10s %-10s %-10s%n", "ID", "Name", "Category", "Price", "Stock");
        System.out.println("------------------------------------------------------------");
        for (Product p : products) {
            System.out.printf("%-5d %-20s %-10s ₹%-9.2f %-10d%n",
                p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock());
        }
    }
    
    
    //

    private static void showAllBills() {
        System.out.println("\n🧾 All Bills:");
        List<Bill> bills = billingService.getAllBills();
        for (Bill bill : bills) {
            printBill(bill);
            System.out.println("---------------------------------------------------");
        }
        if (bills.isEmpty()) {
            System.out.println("No bills found.");
        }
    }

    private static void printBill(Bill bill) {
        System.out.println("Bill ID: " + bill.getId());
        System.out.println("Date: " + bill.getBillDate());
        System.out.println("Customer: " + bill.getCustomer().getName());
        System.out.println("Items:");
        System.out.printf("%-20s %-10s %-10s %-10s%n", "Product", "Quantity", "Price", "Total");
        for (BillItem item : bill.getItems()) {
            double total = item.getQuantity() * item.getPrice();
            System.out.printf("%-20s %-10d ₹%-9.2f ₹%-9.2f%n",
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getPrice(),
                    total);
        }
        System.out.printf("Discount: ₹%.2f%n", bill.getDiscount());
        System.out.printf("Tax: ₹%.2f%n", bill.getTax());
        System.out.printf("Grand Total: ₹%.2f%n", bill.getTotal());
    }

    private static void createNewBill() {
        System.out.print("\nEnter customer name: ");
        String custName = sc.nextLine();
        Customer customer = billingService.addOrGetCustomerByName(custName);
        if (customer == null) {
            System.out.println("❌ Could not find or create customer.");
            return;
        }

        List<BillItem> items = new ArrayList<>();
        boolean addingItems = true;

        while (addingItems) {
            showAllProducts();
            System.out.print("Enter product ID to add to bill (or 0 to finish): ");
            int prodId = getIntInput(0, Integer.MAX_VALUE);

            if (prodId == 0) {
                addingItems = false;
                break;
            }

            Product product = billingService.getProductById(prodId);
            if (product == null) {
                System.out.println("❌ Product not found.");
                continue;
            }
            System.out.printf("Available stock for %s: %d%n", product.getName(), product.getStock());

            System.out.print("Enter quantity to purchase: ");
            int qty = getIntInput(1, product.getStock());
            if (qty > product.getStock()) {
                System.out.println("❌ Insufficient stock. Try again.");
                continue;
            }

            // Check if already added same product, update quantity if so
            BillItem existingItem = items.stream()
                    .filter(i -> i.getProduct().getId() == prodId)
                    .findFirst()
                    .orElse(null);
            if (existingItem != null) {
                if (existingItem.getQuantity() + qty > product.getStock()) {
                    System.out.println("❌ Insufficient stock for total quantity. Try again.");
                    continue;
                }
                items.remove(existingItem);
                items.add(new BillItem(product, existingItem.getQuantity() + qty, product.getPrice()));
            } else {
                items.add(new BillItem(product, qty, product.getPrice()));
            }
        }

        if (items.isEmpty()) {
            System.out.println("❌ No items added to bill.");
            return;
        }

        System.out.print("Enter discount amount (₹), enter 0 if none: ");
        double discount = getDoubleInput(0, Double.MAX_VALUE);

        System.out.print("Enter tax amount (₹), enter 0 if none: ");
        double tax = getDoubleInput(0, Double.MAX_VALUE);

        double subTotal = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        double total = subTotal - discount + tax;
        if (total < 0) total = 0;

        Bill bill = new Bill(customer, discount, tax, total, items);

        if (billingService.createBill(bill)) {
            System.out.println("✅ Bill created successfully!");
            printBill(bill);
        } else {
            System.out.println("❌ Failed to create bill.");
        }
    }

    private static int getIntInput(int min, int max) {
        while (true) {
            String input = sc.nextLine();
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) throw new NumberFormatException();
                return value;
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number between " + min + " and " + max + ": ");
            }
        }
    }

    private static double getDoubleInput(double min, double max) {
        while (true) {
            String input = sc.nextLine();
            try {
                double value = Double.parseDouble(input);
                if (value < min || value > max) throw new NumberFormatException();
                return value;
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a number between " + min + " and " + max + ": ");
            }
        }
    }
}
