package com.example.emailService.consumer;

import com.example.emailService.event.EmailEvent;
import com.example.emailService.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
//listen event-> convert data-> call service
@RetryableTopic(attempts = "4")
public class EmailConsumer {
    private final EmailService emailService;
    private Logger log;

    @KafkaListener(topics="${kafka.topic.email}",groupId="email-id")
    public void consume(EmailEvent emailEvent){
        emailService.sendEmail(emailEvent.getTo(),emailEvent.getSubject(),emailEvent.getBody());
    }

    public void listenDLT(EmailEvent emailEvent){
        log.info("DLT received: {}",emailEvent.getTo());
    }
}
