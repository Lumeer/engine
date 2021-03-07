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

public class SendEmailRequest {

   private final String subject;
   private final String email;
   private final String body;

   public SendEmailRequest(final String subject, final String email, final String body) {
      this.subject = subject;
      this.email = email;
      this.body = body;
   }

   public String getSubject() {
      return subject;
   }

   public String getEmail() {
      return email;
   }

   public String getBody() {
      return body;
   }

   @Override
   public String toString() {
      return "SendEmailRequest{" +
            "subject='" + subject + '\'' +
            ", email='" + email + '\'' +
            ", body='" + body + '\'' +
            '}';
   }
}
