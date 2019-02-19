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
package io.lumeer.core.task;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Function;
import io.lumeer.api.model.User;
import io.lumeer.core.util.PusherClient;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import java.util.Set;

public class FunctionTask implements ContextualTask {

   private User initiator;
   private DaoContextSnapshot daoContextSnapshot;
   private PusherClient pusherClient;

   private Function function;
   private Set<Document> documents;
   private FunctionTask parent;

   public void setFunction(final Function function, final Set<Document> documents, final FunctionTask parent) {
      this.function = function;
      this.documents = documents;
      this.parent = parent;
   }

   @Override
   public ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient) {
      this.initiator = initiator;
      this.daoContextSnapshot = daoContextSnapshot;
      this.pusherClient = pusherClient;

      return this;
   }

   @Override
   public void process() {

      // TODO

      if (parent != null) {
         parent.process();
      }
   }
}
