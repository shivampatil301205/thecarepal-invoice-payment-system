package com.thecarepal.invoices;
import com.razorpay.*;import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.math.*;import java.util.*;
@Service public class PaymentService {
 final InvoiceService invoiceService;final PaymentRepo payments;final EventRepo events;
 @Value("${app.razorpay.key-id}") String keyId;@Value("${app.razorpay.key-secret}") String keySecret;@Value("${app.razorpay.webhook-secret}") String webhookSecret;
 PaymentService(InvoiceService s,PaymentRepo p,EventRepo e){invoiceService=s;payments=p;events=e;}
 public record OrderRequest(Long invoiceId,BigDecimal amount){}
 public record VerifyRequest(String razorpay_order_id,String razorpay_payment_id,String razorpay_signature){}
 static String hmac(String payload,String secret){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 static boolean equal(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
 public Map<String,Object> order(OrderRequest req){
 if(keyId.isBlank()||keySecret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure Razorpay test keys");
 Invoice i=invoiceService.get(req.invoiceId());BigDecimal amount=req.amount()==null?i.balance():req.amount();
 if(amount.signum()<=0||amount.compareTo(i.balance())>0||amount.scale()>2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid amount");
 try{JSONObject data=new JSONObject();data.put("amount",amount.movePointRight(2).longValueExact());data.put("currency","INR");data.put("receipt",i.number+"-"+System.currentTimeMillis());com.razorpay.Order created=new RazorpayClient(keyId,keySecret).orders.create(data);Payment p=new Payment();p.invoiceId=i.id;p.orderId=created.get("id");p.amount=amount;p.status="CREATED";payments.save(p);return Map.of("orderId",p.orderId,"keyId",keyId,"amount",amount,"invoiceId",i.id);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Razorpay order failed: "+e.getMessage());}
 }
 @Transactional public void verify(VerifyRequest req){Payment p=payments.findByOrderId(req.razorpay_order_id()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found"));if(!equal(hmac(req.razorpay_order_id()+"|"+req.razorpay_payment_id(),keySecret),req.razorpay_signature()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid payment signature");
 try{com.razorpay.Payment remote=new RazorpayClient(keyId,keySecret).payments.fetch(req.razorpay_payment_id());String status=remote.get("status");String remoteOrder=remote.get("order_id");Number paise=remote.get("amount");if(!p.orderId.equals(remoteOrder)||!"captured".equals(status)||paise.longValue()!=p.amount.movePointRight(2).longValueExact())throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment not captured or amount mismatch");}catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Could not confirm payment with Razorpay");}
 invoiceService.settle(p,req.razorpay_payment_id());}
 @Transactional public void webhook(String raw,String signature,String eventId){if(webhookSecret.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Webhook secret not configured");if(!equal(hmac(raw,webhookSecret),signature))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid webhook signature");if(eventId==null||eventId.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing event id");if(events.existsById(eventId))return;
 JSONObject json=new JSONObject(raw);if("payment.captured".equals(json.optString("event"))){JSONObject entity=json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");String orderId=entity.getString("order_id");Payment p=payments.findByOrderId(orderId).orElse(null);if(p!=null&&entity.getLong("amount")==p.amount.movePointRight(2).longValueExact()&&"captured".equals(entity.optString("status")))invoiceService.settle(p,entity.getString("id"));}
 WebhookEvent ev=new WebhookEvent();ev.id=eventId;events.save(ev);}
 public List<Payment> forInvoice(Long id){invoiceService.get(id);return payments.findByInvoiceIdOrderByCreatedAtDesc(id).stream().filter(p->"CAPTURED".equals(p.status)).toList();}
}
