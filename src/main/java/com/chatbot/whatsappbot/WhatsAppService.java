package com.chatbot.whatsappbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;

@Service
public class WhatsAppService {

    @Value("${twilio.account.sid}")
    private String accountSid;
    @Value("${twilio.auth.token}")
    private String authToken;
    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        try {
            Twilio.init(accountSid, authToken);
            System.out.println("✅ Twilio initialized with Account SID: " + accountSid);
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Twilio: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Twilio", e);
        }

    }

    // Replace these with your actual values before running
    public void sendText(String to, String messageText) {

        try {
            String formattedTo = "whatsapp:" + to;
            String formattedFrom = "whatsapp:" + twilioPhoneNumber;

            Message message = Message.creator(
                    new PhoneNumber(formattedTo),
                    new PhoneNumber(formattedFrom),
                    messageText
            ).create();

            System.out.println("✅ Message sent successfully: " + message.getSid());
        } catch (Exception e) {
            System.err.println("❌ Failed to send message: " + e.getMessage());
            throw new RuntimeException("Failed to send WhatsApp message", e);
        }
    }
}
