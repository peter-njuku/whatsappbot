package com.chatbot.whatsappbot;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class WebhookController {

    private final WhatsAppService whatsappService;
    private final FAQService faqService;

    public WebhookController(WhatsAppService whatsappService) {
        this.whatsappService = whatsappService;
        this.faqService = new FAQService(); // Initialize FAQ service
    }

    @PostMapping
    public ResponseEntity<String> handleIncomingMessage(
            @RequestParam Map<String, Object> incomingData) {
        try {
            String from = (String) incomingData.get("From");
            String body = (String) incomingData.get("Body");

            if (from == null || body == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            String cleanNumber = from.replace("whatsapp:", "");
            String reply = faqService.findBestAnswer(body);

            whatsappService.sendText(cleanNumber, reply);
            return ResponseEntity.ok("");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing message");
        }
    }
}
