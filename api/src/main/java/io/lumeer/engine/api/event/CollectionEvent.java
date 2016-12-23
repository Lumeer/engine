/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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
package io.lumeer.engine.api.event;

/**
 * An event that occurs when there is an update to a collection.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
abstract public class CollectionEvent {

   private String userName;

   private String internalName;

   public CollectionEvent(final String userName, final String internalName) {
      this.userName = userName;
      this.internalName = internalName;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(final String userName) {
      this.userName = userName;
   }

   public String getInternalName() {
      return internalName;
   }

   public void setInternalName(final String internalName) {
      this.internalName = internalName;
   }
}
