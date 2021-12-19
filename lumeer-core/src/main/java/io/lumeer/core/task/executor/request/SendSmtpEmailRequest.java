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
package io.lumeer.core.task.executor.request;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;

public class SendSmtpEmailRequest extends SendEmailRequest {

   private final String fromName;
   private final SmtpConfiguration smtpConfiguration;
   private Document document;
   private LinkInstance link;
   private String attributeId;

   public SendSmtpEmailRequest(final String subject, final String email, final String body, final String fromName, final SmtpConfiguration smtpConfiguration) {
      super(subject, email, body);

      this.fromName = fromName;
      this.smtpConfiguration = smtpConfiguration;
   }

   public void setAttachment(final Document document, final String attributeId) {
      this.document = document;
      this.attributeId = attributeId;
   }

   public void setAttachment(final LinkInstance link, final String attributeId) {
      this.link = link;
      this.attributeId = attributeId;
   }

   public String getFromName() {
      return fromName;
   }

   public SmtpConfiguration getSmtpConfiguration() {
      return smtpConfiguration;
   }

   public Document getDocument() {
      return document;
   }

   public LinkInstance getLink() {
      return link;
   }

   public String getAttributeId() {
      return attributeId;
   }
}
