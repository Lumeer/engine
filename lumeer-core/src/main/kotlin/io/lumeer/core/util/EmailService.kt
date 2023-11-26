package io.lumeer.core.util

import io.lumeer.core.task.executor.request.SmtpConfiguration
import org.apache.commons.collections4.map.LRUMap
import org.apache.commons.lang3.StringUtils
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.util.logging.Logger
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimeUtility

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
class EmailService(val server: String, val port: Int, val user: String, val password: String, val from: String, val security: EmailSecurityType) {

   private val props: Properties = Properties()
   private val session: Session
   private val log: Logger = Logger.getLogger(EmailService::class.simpleName)

   init {
      props.setProperty("mail.smtp.host", server)
      props.setProperty("mail.smtp.port", port.toString())
      props.setProperty("mail.smtp.from", from)
      props.setProperty("mail.smtp.auth", "true")

      if (security == EmailSecurityType.SSL) {
         props.setProperty("mail.smtp.ssl.enable", "true");
      } else if (security == EmailSecurityType.TLS) {
         props.setProperty("mail.smtp.starttls.enable", "true")
         props.setProperty("mail.smtp.starttls.required", "true")
      }

      session = Session.getInstance(props, object : Authenticator() {
         override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(user, password)
         }
      })
   }

   fun sendEmail(subject: String, to: String, body: String, fromName: String) {
      val message = getMimeMessage(subject, to, body, fromName)
      message.setContent(body, "text/html; charset=utf-8")
      message.saveChanges()

      sendMimeMessage(message)
   }

   fun sendEmail(subject: String, to: String, body: String, fromName: String, attachments: List<EmailPart>) {
      if (attachments.size <= 0) {
         sendEmail(subject, to, body, fromName)
      } else {
         val message = getMimeMessage(subject, to, body, fromName)

         val multipart = MimeMultipart()

         val bodyPart = MimeBodyPart()
         bodyPart.setContent(body, "text/html; charset=utf-8")
         multipart.addBodyPart(bodyPart)

         attachments.forEach { attachment ->
            val part = MimeBodyPart()
            part.setContent(attachment.data, attachment.mimeType)
            part.fileName = attachment.name

            multipart.addBodyPart(part)
         }

         message.setContent(multipart)
         message.saveChanges()

         sendMimeMessage(message)
      }
   }

   private fun getMimeMessage(subject: String, to: String, body: String, fromName: String): MimeMessage {
      val message = MimeMessage(session)
      message.setFrom(InternetAddress(from, if (StringUtils.isNotEmpty(fromName)) "$fromName (Lumeer)" else "Lumeer"))
      message.setSubject(MimeUtility.encodeText(subject, StandardCharsets.UTF_8.name(), "B"))

      val recipients = to.split(",").map { it.trim() }.map { InternetAddress(it) }.toTypedArray()
      message.addRecipients(Message.RecipientType.TO, recipients)

      return message
   }

   private fun sendMimeMessage(message: MimeMessage) {
      val transport = session.getTransport("smtp")
      transport.connect()
      transport.sendMessage(message, message.allRecipients)
      transport.close()
   }

   companion object Factory {

      private val cache = LRUMap<Int, EmailService>(10)

      fun fromSmtpConfiguration(config: SmtpConfiguration): EmailService =
         cache.computeIfAbsent(config.hashCode()) { _ ->
            EmailService(config.host, config.port, config.user, config.password, config.from, config.emailSecurityType)
         }

   }

}