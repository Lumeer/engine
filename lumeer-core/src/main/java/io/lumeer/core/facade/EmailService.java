/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.core.facade;

import io.lumeer.api.model.Language;
import io.lumeer.api.model.User;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import com.floreysoft.jmte.Engine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@ApplicationScoped
public class EmailService {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Logger log;

   private static String SMTP_USER;
   private static String SMTP_PASSWORD;
   private static String SMTP_SERVER;
   private static Integer SMTP_PORT;
   private static String SMTP_FROM;

   private Session session;

   private Map<String, String> subjectLines = new HashMap<>();
   private Map<EmailTemplate, String> templates = new HashMap<>();
   private Engine templateEngine = Engine.createEngine();

   public enum EmailTemplate {
      INVITATION, TASK_ASSIGNED, DUE_DATE_SOON, PAST_DUE_DATE, STATE_UPDATE, TASK_UPDATED, TASK_REMOVED, TASK_UNASSIGNED
   }

   @PostConstruct
   public void init() {
      SMTP_USER = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.SMTP_USER)).orElse("");
      SMTP_PASSWORD = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.SMTP_PASSWORD)).orElse("");
      SMTP_SERVER = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.SMTP_SERVER)).orElse("");
      SMTP_FROM = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.SMTP_FROM)).orElse("");
      try {
         SMTP_PORT = Integer.valueOf(defaultConfigurationProducer.get(DefaultConfigurationProducer.SMTP_PORT));
      } catch (NumberFormatException nfe) {
         SMTP_PORT = 587;
      }

      if (isActive()) {
         final Properties props = new Properties();
         props.setProperty("mail.smtp.host", SMTP_SERVER);
         props.setProperty("mail.smtp.port", SMTP_PORT.toString());
         props.setProperty("mail.smtp.from", SMTP_FROM);
         props.setProperty("mail.smtp.auth", "true");
         //props.setProperty("mail.smtp.ssl.enable", "true");
         props.setProperty("mail.smtp.starttls.enable", "true");
         props.setProperty("mail.smtp.starttls.required", "true");

         this.session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
               return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
         });

         readSubjectLines();
      }
   }

   private boolean isActive() {
      return
            StringUtils.isNotEmpty(SMTP_USER) &&
                  StringUtils.isNotEmpty(SMTP_PASSWORD) &&
                  StringUtils.isNotEmpty(SMTP_SERVER) &&
                  StringUtils.isNotEmpty(SMTP_FROM);
   }

   private void sendEmail(final String subject, final String to, final String body) {
      try {
         final MimeMessage message = new MimeMessage(session);
         message.setFrom(new InternetAddress(SMTP_FROM, "Lumeer"));
         message.addRecipient(Message.RecipientType.TO,  new InternetAddress(to));
         message.setSubject(subject);
         message.setContent(body, "text/html; charset=utf-8");
         message.saveChanges();

         final Transport transport = session.getTransport("smtp");
         transport.connect();
         transport.sendMessage(message, message.getAllRecipients());
         transport.close();
      } catch (Exception e) {
         log.log(Level.SEVERE, "Unable to send email.", e);
      }
   }

   public void sendEmailFromTemplate(final EmailTemplate emailTemplate, final Language language, final String sender, final String recipient) {
      sendEmailFromTemplate(emailTemplate, language, sender, recipient, null);
   }

   public void sendEmailFromTemplate(final EmailTemplate emailTemplate, final Language language, final String sender, final String recipient, final Map<String, Object> additionalData) {
      if (session != null) {
         final String subject = subjectLines.getOrDefault(emailTemplate.toString().toLowerCase() + "_" + language.toString().toLowerCase(), language == Language.EN ? "Hi" : "Dobrý den");
         final String template = loadTemplate(emailTemplate, language);

         if (StringUtils.isNotEmpty(template)) {
            final Map<String, Object> values = new HashMap<>();
            values.put("title", subject); // subject line
            values.put("recipient", recipient); // email
            values.put("userName", sender); // John Doe (john@doe.com)

            if (additionalData != null && additionalData.size() > 0) {
               values.putAll(additionalData);
            }

            final String body = templateEngine.transform(template, values);

            sendEmail(subject, recipient, body);
         }
      }
   }

   public String formatUserReference(final User user) {
      final String userName = user.getName();
      final String userEmail = user.getEmail();

      return StringUtils.isNotEmpty(userName) ?
            userName + " (<a href=\"mailto:" + userEmail + "\" style=\"color: #00B388;\"><span style=\"color: #00B388;\">" + userEmail + "</span></a>)" :
            "<a href=\"mailto:" + userEmail + "\" style=\"color: #00B388;\"><span style=\"color: #00B388;\">" + userEmail + "</span></a>";
   }

   private String loadTemplate(final EmailTemplate emailTemplate, final Language language) {
      final String templateName = "/email-templates/" + emailTemplate.toString().toLowerCase() + "." + language.toString().toLowerCase() + ".html";

      return templates.computeIfAbsent(emailTemplate, key -> {
         try {
            return IOUtils.resourceToString(templateName, StandardCharsets.UTF_8);
         } catch (IOException e) {
            log.log(Level.SEVERE, String.format("Error loading email template '%s': ", emailTemplate.toString().toLowerCase()), e);
         }

         return null;
      });
   }

   private void readSubjectLines() {
      final Properties properties = new Properties();
      try {
         final InputStream input = EmailService.class.getResourceAsStream("/email-templates/subject.properties");
         if (input != null) {
            properties.load(new InputStreamReader(input));
            properties.forEach((key, value) -> {
               subjectLines.put(key.toString(), value.toString());
            });
         } else {
            log.log(Level.WARNING, "Email subject lines file not found.");
         }
      } catch (IOException e) {
         log.log(Level.SEVERE, "Unable to load email subject lines: ", e);
      }
   }
}
