/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.api.push;

/**
 * Push message that can be sent to a connected client.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class PushMessage {

   /**
    * Type of the message.
    */
   private final String type;

   /**
    * Resource identifier, e.g. "document:&lt;document id&gt;", "collection:&lt;collection name&gt;"...
    */
   private final String resource;

   /**
    * Any textual message.
    */
   private final String message;

   /**
    * Specifies a new push message for the client.
    *
    * @param type
    *       Type of the message.
    * @param resource
    *       Resource address.
    * @param message
    *       Message content.
    */
   public PushMessage(final String type, final String resource, final String message) {
      this.type = type;
      this.resource = resource;
      this.message = message;
   }

   public String getType() {
      return type;
   }

   public String getResource() {
      return resource;
   }

   public String getMessage() {
      return message;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final PushMessage that = (PushMessage) o;

      if (type != null ? !type.equals(that.type) : that.type != null) {
         return false;
      }
      if (resource != null ? !resource.equals(that.resource) : that.resource != null) {
         return false;
      }
      return message != null ? message.equals(that.message) : that.message == null;

   }

   @Override
   public int hashCode() {
      int result = type != null ? type.hashCode() : 0;
      result = 31 * result + (resource != null ? resource.hashCode() : 0);
      result = 31 * result + (message != null ? message.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "{ type: \""
            + (type == null ? "" : type)
            + "\", resource: \""
            + (resource == null ? "" : resource)
            + ", message: \""
            + (message == null ? "" : message)
            + "\" }";
   }
}
