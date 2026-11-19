package com.thecarepal.invoices;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
@RestController @RequestMapping("/api") @CrossOrigin(origins="${app.frontend-url}") public class ApiController {
 final InvoiceService invoices;final PaymentService payments;
 ApiController(InvoiceService i,PaymentService p){invoices=i;payments=p;}
 @GetMapping("/invoices") public List<InvoiceService.InvoiceView> all(){return invoices.all();}
 @GetMapping("/invoices/{id}") public InvoiceService.InvoiceView one(@PathVariable Long id){return invoices.one(id);}
 @PostMapping("/invoices") public InvoiceService.InvoiceView create(@RequestBody InvoiceService.CreateInput input){return invoices.create(input);}
 @GetMapping("/invoices/{id}/payments") public List<Payment> receipts(@PathVariable Long id){return payments.forInvoice(id);}
 @PostMapping("/payments/order") public Map<String,Object> order(@RequestBody PaymentService.OrderRequest req){return payments.order(req);}
 @PostMapping("/payments/verify") public Map<String,String> verify(@RequestBody PaymentService.VerifyRequest req){payments.verify(req);return Map.of("message","Payment verified");}
 @PostMapping("/payments/webhook") public Map<String,String> webhook(@RequestBody String body,@RequestHeader(value="X-Razorpay-Signature",required=false) String sig,@RequestHeader(value="x-razorpay-event-id",required=false) String eventId){payments.webhook(body,sig,eventId);return Map.of("message","ok");}
 @GetMapping(value="/invoices/{id}/pdf",produces="application/pdf") public ResponseEntity<byte[]> pdf(@PathVariable Long id)throws Exception{
 var i=invoices.one(id);ByteArrayOutputStream out=new ByteArrayOutputStream();Document doc=new Document();PdfWriter.getInstance(doc,out);doc.open();doc.add(new Paragraph("THECAREPAL | INVOICE",FontFactory.getFont(FontFactory.HELVETICA_BOLD,20)));doc.add(new Paragraph("Invoice: "+i.number()));doc.add(new Paragraph("Customer: "+i.customerName()+" ("+i.customerEmail()+")"));doc.add(new Paragraph("Issued: "+i.issuedOn()+"     Due: "+i.dueOn()));doc.add(new Paragraph(" "));
 PdfPTable t=new PdfPTable(new float[]{3,5,2});t.setWidthPercentage(100);for(String h:new String[]{"Date","Care service","Rate (INR)"})t.addCell(h);for(CareDay d:i.careDays()){t.addCell(d.careDate.toString());t.addCell(d.description==null?"Care service":d.description);t.addCell(d.rate.toPlainString());}doc.add(t);
 for(String line:List.of("Subtotal: INR "+i.subtotal(),"Discount: INR "+i.discount(),"GST: INR "+i.gst(),"Total: INR "+i.total(),"Paid: INR "+i.paid(),"Balance: INR "+i.balance(),"Status: "+i.status()))doc.add(new Paragraph(line));doc.close();return ResponseEntity.ok()
    .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + i.number() + ".pdf\""
    )
    .body(out.toByteArray());}
}
