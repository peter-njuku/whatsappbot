package com.chatbot.whatsappbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
public class WhatsappbotApplication {
	public static void main(String[] args) {
		SpringApplication.run(WhatsappbotApplication.class, args);
	}
}
