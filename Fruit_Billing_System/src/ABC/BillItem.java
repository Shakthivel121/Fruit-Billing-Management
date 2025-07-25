package ABC;

public class BillItem {
    private int id;
    private int billId;
    private Product product;
    private int quantity;
    private double price;

    public BillItem(int id, int billId, Product product, int quantity, double price) {
        this.id = id;
        this.billId = billId;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public BillItem(Product product, int quantity, double price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public int getId() { return id; }
    public int getBillId() { return billId; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}
