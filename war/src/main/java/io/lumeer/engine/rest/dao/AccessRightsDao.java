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
package io.lumeer.engine.rest.dao;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class AccessRightsDao {
   private boolean read;
   private boolean write;
   private boolean execute;
   private String userName;

   public AccessRightsDao() {
   }

   public AccessRightsDao(final boolean read, final boolean write, final boolean execute, final String userName) {
      this.read = read;
      this.write = write;
      this.execute = execute;
      this.userName = userName;
   }

   public boolean isRead() {
      return read;
   }

   public void setRead(final boolean read) {
      this.read = read;
   }

   public boolean isWrite() {
      return write;
   }

   public void setWrite(final boolean write) {
      this.write = write;
   }

   public boolean isExecute() {
      return execute;
   }

   public void setExecute(final boolean execute) {
      this.execute = execute;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(final String userName) {
      this.userName = userName;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final AccessRightsDao that = (AccessRightsDao) o;

      if (read != that.read) {
         return false;
      }
      if (write != that.write) {
         return false;
      }
      if (execute != that.execute) {
         return false;
      }
      return userName != null ? userName.equals(that.userName) : that.userName == null;
   }

   @Override
   public int hashCode() {
      int result = (read ? 1 : 0);
      result = 31 * result + (write ? 1 : 0);
      result = 31 * result + (execute ? 1 : 0);
      result = 31 * result + (userName != null ? userName.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "AccessRightsDao{" +
            "read=" + read +
            ", write=" + write +
            ", execute=" + execute +
            ", userName='" + userName + '\'' +
            '}';
   }
}
