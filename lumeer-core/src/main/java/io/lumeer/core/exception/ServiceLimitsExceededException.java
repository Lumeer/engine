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
package io.lumeer.core.exception;

import io.lumeer.api.exception.LumeerException;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.common.Resource;

/**
 * Used when user tries to create more resources than allowed.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ServiceLimitsExceededException extends LumeerException {
   private final Resource resource;

   public ServiceLimitsExceededException(final long limit, final Resource resource) {
      super("Cannot create another " + resource.getType().toString().toLowerCase() + ". You are only allowed to have " + limit + " of them. Update your service level in organization settings.");
      this.resource = resource;
   }

   public ServiceLimitsExceededException(final long limit, final long dataSize) {
      super("Cannot create another document. Your database now occupies " + dataSize + "MB. You are only allowed to have " + limit + "MB of data. Update your service level in organization settings.");
      this.resource = null;
   }

   public ServiceLimitsExceededException(final long limit, final long documents, final Document document) {
      super("Cannot create another document. Your database now contains " + documents + " (including supporting system documents). You are only allowed to have " + limit + " documents. Update your service level in organization settings.");
      this.resource = null;
   }

   public ServiceLimitsExceededException(final int usersLimit) {
      super("Cannot invite another user. You are only allowed to have " + usersLimit + " users. Update your service level in organization settings.");
      this.resource = null;
   }

   public Resource getResource() {
      return resource;
   }
}
