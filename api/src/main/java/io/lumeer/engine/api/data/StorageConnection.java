/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.api.data;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Credentials and URL to get a database connection.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class StorageConnection implements Serializable {

   private static final long serialVersionUID = -5170090833580954480L;

   private final String host;
   private final int port;
   private final String userName;
   private final char[] password;

   /**
    * Defines a new connection information using the given hostname, port, user name and password.
    *
    * @param host
    *       The host name.
    * @param port
    *       The TCP port.
    * @param userName
    *       The user name.
    * @param password
    *       The user password.
    */
   public StorageConnection(final String host, final int port, final String userName, final String password) {
      this.host = host;
      this.port = port;
      this.userName = userName;
      this.password = password.toCharArray();
   }

   public String getHost() {
      return host;
   }

   public int getPort() {
      return port;
   }

   public String getUserName() {
      return userName;
   }

   public char[] getPassword() {
      return password;
   }

   @Override
   public String toString() {
      return "StorageConnection{"
            + "host='" + host + '\''
            + ", port=" + port
            + ", userName='" + userName + '\''
            + ", password=" + new String(password)
            + '}';
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final StorageConnection that = (StorageConnection) o;

      if (port != that.port) {
         return false;
      }
      if (!host.equals(that.host)) {
         return false;
      }
      if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
         return false;
      }
      return Arrays.equals(password, that.password);
   }

   @Override
   public int hashCode() {
      int result = host.hashCode();
      result = 31 * result + port;
      result = 31 * result + (userName != null ? userName.hashCode() : 0);
      result = 31 * result + Arrays.hashCode(password);
      return result;
   }
}
