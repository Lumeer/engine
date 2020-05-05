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

import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import org.apache.commons.lang3.StringUtils;

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
public class EmailFacade {

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private AuthenticatedUser user;

   @Inject
   private Logger log;

   private static String SMTP_USER;
   private static String SMTP_PASSWORD;
   private static String SMTP_SERVER;
   private static Integer SMTP_PORT;
   private static String SMTP_FROM;

   private Session session;

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

   public void sendInvitation(final String invitedEmail) {
      if (session != null) {
         final String userName = user.getUserName();
         final String userEmail = user.getUserEmail();
         final String locale = requestDataKeeper.getUserLocale();
         final String subject = StringUtils.isNotEmpty(locale) && "cs".equals(locale) ? "Pozvánka ke spolupráci" : "Invitation for collaboration";
         final String bodyTemplate =
               StringUtils.isNotEmpty(locale) && "cs".equals(locale)
                     ? "Dobrý den,<br/><br/>Váš kolega %s Vás zve ke spolupráci do Lumeera<br/><a href=\"https://get.lumeer.io/cs/\">https://get.lumeer.io/</a><br/><br/>Hezký den,<br/>Tým Lumeer<br/>"
                     : "Hello,<br/><br/>your colleague %s has invited you to collaborate in Lumeer<br/><a href=\"https://get.lumeer.io/en/\">https://get.lumeer.io/</a><br/><br/>Cheers,<br/>Lumeer Team<br/>";
         final String colleague = StringUtils.isNotEmpty(userName) ? userName + " (" + userEmail + ")" : userEmail;
         final String body = String.format(bodyTemplate, colleague);

         sendEmail(subject, invitedEmail, body);
      }
   }

}
