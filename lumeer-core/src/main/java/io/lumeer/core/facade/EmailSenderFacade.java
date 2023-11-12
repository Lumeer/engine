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
import io.lumeer.core.util.EmailSecurityType;
import io.lumeer.core.util.EmailService;

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
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@ApplicationScoped
public class EmailSenderFacade {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private Logger log;

   private static String SMTP_USER;
   private static String SMTP_PASSWORD;
   private static String SMTP_SERVER;
   private static Integer SMTP_PORT;
   private static String SMTP_FROM;

   private Map<String, String> subjectLines = new HashMap<>();
   private Map<String, String> templates = new HashMap<>();
   private Engine templateEngine = Engine.createEngine();
   private EmailService emailService = null;

   public enum EmailTemplate {
      INVITATION, TASK_ASSIGNED, DUE_DATE_SOON, PAST_DUE_DATE, STATE_UPDATE, TASK_UPDATED, TASK_REMOVED, TASK_UNASSIGNED, ORGANIZATION_SHARED, PROJECT_SHARED, COLLECTION_SHARED, VIEW_SHARED, DUE_DATE_CHANGED, TASK_COMMENTED, TASK_MENTIONED, TASK_REOPENED;
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
         emailService = new EmailService(SMTP_SERVER, SMTP_PORT, SMTP_USER, SMTP_PASSWORD, SMTP_FROM, EmailSecurityType.TLS);
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

   public void sendEmailFromTemplate(final EmailTemplate emailTemplate, final Language language, final String sender, final String from, final String recipient, final String subjectPart) {
      sendEmailFromTemplate(emailTemplate, language, sender, from, recipient, subjectPart, null);
   }

   public void sendEmailFromTemplate(final EmailTemplate emailTemplate, final Language language, final String sender, final String from, final String recipient, final String subjectPart, final Map<String, Object> additionalData) {
      if (emailService != null) {
         final String subject = String.format(subjectLines.getOrDefault(emailTemplate.toString().toLowerCase() + "_" + language.toString().toLowerCase(), language == Language.EN ? "Hi" : "Dobrý den"), subjectPart);
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

            try {
               emailService.sendEmail(subject, recipient, body, from);
            } catch (Exception e) {
               log.log(Level.SEVERE, String.format("Unable to send email '%s'.", subject), e);
            }
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

   public String formatFrom(final User user) {
      final String userName = user.getName();
      final String userEmail = user.getEmail();

      return StringUtils.isNotEmpty(userName) ? userName : userEmail;
   }

   private String loadTemplate(final EmailTemplate emailTemplate, final Language language) {
      final String templateName = "/email-templates/" + emailTemplate.toString().toLowerCase() + "." + language.toString().toLowerCase() + ".html";

      return templates.computeIfAbsent(templateName, key -> {
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
         final InputStream input = EmailSenderFacade.class.getResourceAsStream("/email-templates/subject.properties");
         if (input != null) {
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8.name()));
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
