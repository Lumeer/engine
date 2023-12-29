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
package io.lumeer.engine.api.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Credentials and URL to get a database connection.
 */
public class StorageConnection implements Serializable {

   private static final long serialVersionUID = -5170090833580954480L;

   private String connectionString;
   private String host;
   private int port;
   private String userName;
   private char[] password;

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

   /**
    * Defines a new connection information based on a connection string.
    * @param connectionString The connection string per DB specification.
    */
   public StorageConnection(final String connectionString) {
      this.connectionString = connectionString;
   }

   public String getConnectionString() {
      return connectionString;
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
      return "StorageConnection{" +
            "connectionString='" + connectionString + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            ", userName='" + userName + '\'' +
            ", password=" + Arrays.toString(password) +
            '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      StorageConnection that = (StorageConnection) o;
      return port == that.port && Objects.equals(connectionString, that.connectionString) && Objects.equals(host, that.host) && Objects.equals(userName, that.userName) && Arrays.equals(password, that.password);
   }

   @Override
   public int hashCode() {
      int result = Objects.hash(connectionString, host, port, userName);
      result = 31 * result + Arrays.hashCode(password);
      return result;
   }
}
