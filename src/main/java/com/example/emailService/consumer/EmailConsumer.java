package com.example.emailService.consumer;

import com.example.emailService.event.EmailEvent;
import com.example.emailService.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
//listen event-> convert data-> call service
public class EmailConsumer {
    private final EmailService emailService;

    @KafkaListener(topics="${kafka.topic.email}",groupId="email-id")
    public void consume(EmailEvent emailEvent){
        emailService.sendEmail(emailEvent.getTo(),emailEvent.getSubject(),emailEvent.getBody());
    }
}
