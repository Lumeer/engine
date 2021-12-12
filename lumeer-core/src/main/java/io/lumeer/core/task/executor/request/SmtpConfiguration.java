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

import io.lumeer.core.util.EmailSecurityType;

import java.util.Objects;

public class SmtpConfiguration {

   private final String host;
   private final Integer port;
   private final String user;
   private final String password;
   private final String from;
   private final EmailSecurityType emailSecurityType;

   public SmtpConfiguration(final String host, final Integer port, final String user, final String password, final String from, final EmailSecurityType emailSecurityType) {
      this.host = host;
      this.port = port;
      this.user = user;
      this.password = password;
      this.from = from;
      this.emailSecurityType = emailSecurityType;
   }

   public String getHost() {
      return host;
   }

   public Integer getPort() {
      return port;
   }

   public String getUser() {
      return user;
   }

   public String getPassword() {
      return password;
   }

   public String getFrom() {
      return from;
   }

   public EmailSecurityType getEmailSecurityType() {
      return emailSecurityType;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final SmtpConfiguration that = (SmtpConfiguration) o;
      return Objects.equals(host, that.host) && Objects.equals(port, that.port) && Objects.equals(user, that.user) && Objects.equals(password, that.password) && Objects.equals(from, that.from) && emailSecurityType == that.emailSecurityType;
   }

   @Override
   public int hashCode() {
      return Objects.hash(host, port, user, password, from, emailSecurityType);
   }

   @Override
   public String toString() {
      return "SmtpConfiguration{" +
            "host='" + host + '\'' +
            ", port=" + port +
            ", user='" + user + '\'' +
            ", password='" + password + '\'' +
            ", from='" + from + '\'' +
            ", emailSecurityType=" + emailSecurityType +
            '}';
   }
}
