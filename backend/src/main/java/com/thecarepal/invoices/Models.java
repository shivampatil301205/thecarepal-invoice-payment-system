package com.thecarepal.invoices;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
class Models { private Models(){} }
@Entity class Invoice {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true,nullable=false) public String number;
 public String customerName; public String customerEmail; public LocalDate issuedOn; public LocalDate dueOn;
 @Column(precision=14,scale=2) public BigDecimal subtotal=BigDecimal.ZERO,discount=BigDecimal.ZERO,gst=BigDecimal.ZERO,total=BigDecimal.ZERO,paid=BigDecimal.ZERO;
 @Version public Long version;
 public String status(){if(paid.compareTo(total)>=0)return "PAID";return dueOn.isBefore(LocalDate.now())?"OVERDUE":"DUE";}
 public BigDecimal balance(){return total.subtract(paid).max(BigDecimal.ZERO);}
}
@Entity class CareDay { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public Long invoiceId; public LocalDate careDate; public String description; @Column(precision=14,scale=2) public BigDecimal rate; }
@Entity class Payment { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public Long invoiceId; @Column(unique=true) public String orderId; @Column(unique=true) public String paymentId; @Column(precision=14,scale=2) public BigDecimal amount; public String status; public Instant createdAt=Instant.now(); }
@Entity class WebhookEvent { @Id public String id; public Instant processedAt=Instant.now(); }
