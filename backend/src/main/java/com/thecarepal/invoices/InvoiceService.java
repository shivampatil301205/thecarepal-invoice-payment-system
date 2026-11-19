package com.thecarepal.invoices;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;import java.time.*;import java.util.*;
@Service public class InvoiceService {
 final InvoiceRepo invoices;final CareRepo days;final PaymentRepo payments;
 InvoiceService(InvoiceRepo i,CareRepo d,PaymentRepo p){invoices=i;days=d;payments=p;}
 public record DayInput(LocalDate date,String description,BigDecimal rate){}
 public record CreateInput(String customerName,String customerEmail,LocalDate dueDate,BigDecimal discountPercent,BigDecimal gstPercent,List<DayInput> careDays){}
 public record InvoiceView(Long id,String number,String customerName,String customerEmail,LocalDate issuedOn,LocalDate dueOn,BigDecimal subtotal,BigDecimal discount,BigDecimal gst,BigDecimal total,BigDecimal paid,BigDecimal balance,String status,List<CareDay> careDays){}
 Invoice get(Long id){return invoices.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Invoice not found"));}
 InvoiceView view(Invoice i){return new InvoiceView(i.id,i.number,i.customerName,i.customerEmail,i.issuedOn,i.dueOn,i.subtotal,i.discount,i.gst,i.total,i.paid,i.balance(),i.status(),days.findByInvoiceIdOrderByCareDateAsc(i.id));}
 public List<InvoiceView> all(){return invoices.findAll().stream().sorted(Comparator.comparing((Invoice i)->i.id).reversed()).map(this::view).toList();}
 public InvoiceView one(Long id){return view(get(id));}
 static BigDecimal money(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP);}
 @Transactional public InvoiceView create(CreateInput in){
 if(in.customerName()==null||in.customerName().isBlank()||in.customerEmail()==null||!in.customerEmail().contains("@")||in.dueDate()==null||in.careDays()==null||in.careDays().isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Customer, email, due date and care days required");
 BigDecimal dp=in.discountPercent()==null?BigDecimal.ZERO:in.discountPercent(),gp=in.gstPercent()==null?BigDecimal.ZERO:in.gstPercent();
 if(dp.compareTo(BigDecimal.ZERO)<0||dp.compareTo(new BigDecimal("100"))>0||gp.compareTo(BigDecimal.ZERO)<0||gp.compareTo(new BigDecimal("100"))>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Percent must be 0-100");
 BigDecimal sum=BigDecimal.ZERO;for(DayInput d:in.careDays()){if(d.date()==null||d.rate()==null||d.rate().signum()<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid care day");sum=sum.add(d.rate());}
 Invoice i=new Invoice();i.customerName=in.customerName();i.customerEmail=in.customerEmail();i.issuedOn=LocalDate.now();i.dueOn=in.dueDate();i.subtotal=money(sum);i.discount=money(sum.multiply(dp).divide(new BigDecimal("100"),4,RoundingMode.HALF_UP));i.gst=money(sum.subtract(i.discount).multiply(gp).divide(new BigDecimal("100"),4,RoundingMode.HALF_UP));i.total=i.subtotal.subtract(i.discount).add(i.gst);i.number="INV-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();invoices.save(i);
 for(DayInput d:in.careDays()){CareDay c=new CareDay();c.invoiceId=i.id;c.careDate=d.date();c.description=d.description();c.rate=money(d.rate());days.save(c);}return view(i);
 }
 @Transactional public void settle(Payment p,String paymentId){
 if("CAPTURED".equals(p.status))return;
 if(payments.findByPaymentId(paymentId).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment already used");
 Invoice i=invoices.locked(p.invoiceId).orElseThrow();
 p.paymentId=paymentId;p.status="CAPTURED";i.paid=i.paid.add(p.amount).min(i.total);invoices.save(i);payments.save(p);
 }
}
