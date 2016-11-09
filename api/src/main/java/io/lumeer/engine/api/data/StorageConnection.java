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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class StorageConnection {

   final private String host;
   final private int port;
   final private String userName;
   final private char[] password;

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
}
