package ABC;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BillingService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/billing_db";
    private static final String USER = "ABC";  //db name
    private static final String PASS = "0000";  // db pass

    public BillingService() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

// user login with pass check
    public User login(String username, String passwordHash) {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                    );
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Product

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                products.add(new Product(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock")
                ));
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public boolean updateProductPrice(int productId, double newPrice) {
        String sql = "UPDATE products SET price = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateProductStock(int productId, int newStock) {
        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newStock);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Product getProductById(int productId) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                    );
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Customer
    public Customer addOrGetCustomerByName(String name) {
        // Check if exists:
        String select = "SELECT * FROM customers WHERE name = ?";
        String insert = "INSERT INTO customers (name) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            try (PreparedStatement selStmt = conn.prepareStatement(select)) {
                selStmt.setString(1, name);
                try (ResultSet rs = selStmt.executeQuery()) {
                    if(rs.next()) {
                        return new Customer(rs.getInt("id"), rs.getString("name"));
                    }
                }
            }

            try (PreparedStatement insStmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                insStmt.setString(1, name);
                int affected = insStmt.executeUpdate();
                if(affected == 1) {
                    try (ResultSet keys = insStmt.getGeneratedKeys()) {
                        if(keys.next()) {
                            return new Customer(keys.getInt(1), name);
                        }
                    }
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Billing

    public boolean createBill(Bill bill) {
        String insertBill = "INSERT INTO bills (customer_id, discount, tax, total) VALUES (?, ?, ?, ?)";
        String insertBillItem = "INSERT INTO bill_items (bill_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        String updateStock = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            conn.setAutoCommit(false);

            try (PreparedStatement billStmt = conn.prepareStatement(insertBill, Statement.RETURN_GENERATED_KEYS)) {
                billStmt.setInt(1, bill.getCustomer().getId());
                billStmt.setDouble(2, bill.getDiscount());
                billStmt.setDouble(3, bill.getTax());
                billStmt.setDouble(4, bill.getTotal());

                int affected = billStmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                int billId;
                try (ResultSet keys = billStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        billId = keys.getInt(1);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }

                // Insert bill items & update stock
                for (BillItem item : bill.getItems()) {
                    // Check and update stock
                    try (PreparedStatement stockStmt = conn.prepareStatement(updateStock)) {
                        stockStmt.setInt(1, item.getQuantity());
                        stockStmt.setInt(2, item.getProduct().getId());
                        stockStmt.setInt(3, item.getQuantity());
                        int stockUpdated = stockStmt.executeUpdate();
                        if (stockUpdated == 0) {
                            // Not enough stock
                            conn.rollback();
                            System.out.println("❌ Not enough stock for product: " + item.getProduct().getName());
                            return false;
                        }
                    }

                    // Insert bill itms
                    try (PreparedStatement itemStmt = conn.prepareStatement(insertBillItem)) {
                        itemStmt.setInt(1, billId);
                        itemStmt.setInt(2, item.getProduct().getId());
                        itemStmt.setInt(3, item.getQuantity());
                        itemStmt.setDouble(4, item.getPrice());
                        itemStmt.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //display bills
    public List<Bill> getAllBills() {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, c.name AS customer_name FROM bills b JOIN customers c ON b.customer_id = c.id ORDER BY b.bill_date DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while(rs.next()) {
                int billId = rs.getInt("id");
                Customer customer = new Customer(rs.getInt("customer_id"), rs.getString("customer_name"));
                Timestamp billDate = rs.getTimestamp("bill_date");
                double discount = rs.getDouble("discount");
                double tax = rs.getDouble("tax");
                double total = rs.getDouble("total");

                List<BillItem> items = getBillItemsByBillId(billId);

                bills.add(new Bill(billId, customer, billDate, discount, tax, total, items));
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return bills;
    }

    private List<BillItem> getBillItemsByBillId(int billId) {
        List<BillItem> items = new ArrayList<>();
        String sql = "SELECT bi.*, p.name, p.category FROM bill_items bi JOIN products p ON bi.product_id = p.id WHERE bi.bill_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, billId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        0,  // price from bill item not product table here
                        0   // stock not relevant here
                    );
                    items.add(new BillItem(
                        rs.getInt("id"),
                        billId,
                        product,
                        rs.getInt("quantity"),
                        rs.getDouble("price")
                    ));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}
