package com.artaeum.uaa.service;

import com.artaeum.uaa.domain.User;
import org.apache.commons.codec.CharEncoding;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.internet.MimeMessage;
import java.util.Locale;

@Service
public class MailService {

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    private Environment env;

    private JavaMailSender javaMailSender;

    private MessageSource messageSource;

    private SpringTemplateEngine templateEngine;

    public MailService(Environment env,
                       JavaMailSender javaMailSender,
                       MessageSource messageSource,
                       SpringTemplateEngine templateEngine) {
        this.env = env;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    public void sendActivationEmail(User user) {
        this.sendEmailFromTemplate(user, "activationEmail", "email.activation.title");
    }

    public void sendCreationEmail(User user) {
        this.sendEmailFromTemplate(user, "creationEmail", "email.activation.title");
    }

    public void sendPasswordResetMail(User user) {
        this.sendEmailFromTemplate(user, "passwordResetEmail", "email.reset.title");
    }

    private void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, this.env.getProperty("artaeum.mail.url"));
        String content = this.templateEngine.process(templateName, context);
        String subject = this.messageSource.getMessage(titleKey, null, locale);
        this.sendEmail(user.getEmail(), subject, content);
    }

    private void sendEmail(String to, String subject, String content) {
        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        try {
            new MimeMessageHelper(mimeMessage, false, CharEncoding.UTF_8) {{
                setTo(to);
                setFrom(env.getProperty("spring.mail.username"));
                setSubject(subject);
                setText(content, true);
            }};
            this.javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
