package ABC;

import java.sql.Timestamp;
import java.util.List;

public class Bill {
    private int id;
    private Customer customer;
    private Timestamp billDate;
    private double discount;
    private double tax;
    private double total;
    private List<BillItem> items;

    public Bill(int id, Customer customer, Timestamp billDate, double discount, double tax, double total, List<BillItem> items) {
        this.id = id;
        this.customer = customer;
        this.billDate = billDate;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.items = items;
    }

    public Bill(Customer customer, double discount, double tax, double total, List<BillItem> items) {
        this.customer = customer;
        this.discount = discount;
        this.tax = tax;
        this.total = total;
        this.items = items;
    }

    public int getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Timestamp getBillDate() { return billDate; }
    public double getDiscount() { return discount; }
    public double getTax() { return tax; }
    public double getTotal() { return total; }
    public List<BillItem> getItems() { return items; }
}
