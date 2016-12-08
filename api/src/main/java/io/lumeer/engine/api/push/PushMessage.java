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
