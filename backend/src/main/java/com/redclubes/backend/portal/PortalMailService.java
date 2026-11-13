package com.redclubes.backend.portal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;

@Service
public class PortalMailService {
    private final JavaMailSender sender;
    private final ThreadPoolTaskExecutor executor;
    private final String from;
    PortalMailService(JavaMailSender sender, @Qualifier("portalMailExecutor") ThreadPoolTaskExecutor executor,
                      @Value("${portal.mail-from:noreply@example.invalid}") String from) {
        this.sender = sender; this.executor = executor; this.from = from;
    }
    public void enqueue(String email, String code) {
        try {
            executor.execute(() -> {
                try {
                    var message = new SimpleMailMessage();
                    message.setFrom(from); message.setTo(email);
                    message.setSubject("Codigo de acceso - Bomberos");
                    message.setText("Tu codigo de acceso es " + code + ". Vence en 5 minutos. No lo compartas.");
                    sender.send(message);
                } catch (Exception e) {
                    // Provider exceptions may contain recipients and credentials; never log them.
                    LoggerFactory.getLogger(PortalMailService.class).warn("No se pudo entregar un correo del portal");
                }
            });
        } catch (org.springframework.core.task.TaskRejectedException e) {
            LoggerFactory.getLogger(PortalMailService.class).warn("Cola SMTP del portal completa");
        }
    }
}
