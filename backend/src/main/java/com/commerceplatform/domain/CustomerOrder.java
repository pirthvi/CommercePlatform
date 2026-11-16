package com.commerceplatform.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 36)
    private String orderNumber;
    @Column(nullable = false, length = 140)
    private String customerName;
    @Column(nullable = false, length = 180)
    private String email;
    @Column(nullable = false, length = 40)
    private String phone;
    @Column(nullable = false, length = 300)
    private String address;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, length = 24)
    private String postalCode;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shipping;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Column(nullable = false, length = 24)
    private String status = "PROCESSING";
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerAccount customer;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> items = new ArrayList<>();

    @Column(length = 3) private String currency;
    public String getCurrency() { return currency == null ? "PKR" : currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    protected CustomerOrder() {}
    public CustomerOrder(String orderNumber, String customerName, String email, String phone, String address, String city, String postalCode, BigDecimal subtotal, BigDecimal shipping, BigDecimal total, CustomerAccount customer) {
        this.orderNumber = orderNumber; this.customerName = customerName; this.email = email; this.phone = phone;
        this.address = address; this.city = city; this.postalCode = postalCode;
        this.subtotal = subtotal; this.shipping = shipping; this.total = total;
        this.customer = customer;
    }
    public void addItem(OrderLine item) { items.add(item); item.setOrder(this); }
    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getPostalCode() { return postalCode; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getShipping() { return shipping; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    @JsonIgnore public CustomerAccount getCustomer() { return customer; }
    public List<OrderLine> getItems() { return items; }
}
