package com.thecarepal.invoices;
import org.springframework.boot.CommandLineRunner;import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.beans.factory.annotation.Value;import org.springframework.mail.SimpleMailMessage;import org.springframework.mail.javamail.JavaMailSender;
import java.time.*;import java.math.*;import java.util.*;
@Component public class SeedAndReminder implements CommandLineRunner {
 final InvoiceService service;final InvoiceRepo repo;final JavaMailSender mail;
 @Value("${app.reminders.enabled:false}") boolean enabled;
 SeedAndReminder(InvoiceService s,InvoiceRepo r,JavaMailSender m){service=s;repo=r;mail=m;}
 public void run(String...args){if(repo.count()!=0)return;for(int k=0;k<3;k++){List<InvoiceService.DayInput> days=new ArrayList<>();for(int j=0;j<5;j++)days.add(new InvoiceService.DayInput(LocalDate.now().minusDays(10-j),"Home nursing care",new BigDecimal("1500")));service.create(new InvoiceService.CreateInput(new String[]{"Aarav Sharma","Priya Mehta","Rohan Iyer"}[k],new String[]{"aarav@example.com","priya@example.com","rohan@example.com"}[k],LocalDate.now().plusDays(new int[]{7,-3,14}[k]),new BigDecimal("5"),new BigDecimal("18"),days));}}
 @Scheduled(cron="0 0 9 * * *",zone="Asia/Kolkata") public void remind(){if(!enabled)return;for(Invoice i:repo.findAll()){if(!"OVERDUE".equals(i.status()))continue;try{SimpleMailMessage m=new SimpleMailMessage();m.setTo(i.customerEmail);m.setSubject("Invoice overdue: "+i.number);m.setText("Your invoice "+i.number+" has a balance of INR "+i.balance()+". Please visit the member portal.");mail.send(m);}catch(Exception e){System.err.println("Reminder failed for "+i.number+": "+e.getMessage());}}}
}
