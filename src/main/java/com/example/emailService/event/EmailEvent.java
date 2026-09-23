package com.example.emailService.event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    /*
     * Set by the producer -- by OutboxWriter on the registration path, by
     * EmailProducer on the forgot-password one. EmailConsumer dedups on it.
     *
     * Null on anything published before this field existed. This class is a
     * local copy of common's EmailEvent: email-service does not depend on
     * common, so the two have to be kept in step by hand and the field names
     * have to match for Jackson.
     */
    private String eventId;
    private String to; //to consumer
    private String subject;
    private String body; //main content
    private String eventType; //activate or forgot password
    private Map<String, String> metadata; // Optional extra data
}

