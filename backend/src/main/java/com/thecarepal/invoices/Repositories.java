package com.thecarepal.invoices;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
interface InvoiceRepo extends JpaRepository<Invoice,Long> { @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Invoice i where i.id=:id") Optional<Invoice> locked(Long id); }
interface CareRepo extends JpaRepository<CareDay,Long> {List<CareDay> findByInvoiceIdOrderByCareDateAsc(Long invoiceId);}
interface PaymentRepo extends JpaRepository<Payment,Long> {Optional<Payment> findByOrderId(String orderId); Optional<Payment> findByPaymentId(String paymentId); List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);}
interface EventRepo extends JpaRepository<WebhookEvent,String> {}
