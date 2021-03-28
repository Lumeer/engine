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
package io.lumeer.core.task;

import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.ConfigurationFacade;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.storage.api.dao.context.DaoContextSnapshotFactory;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ContextualTaskFactory {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private DaoContextSnapshotFactory daoContextSnapshotFactory;

   @Inject
   private PusherFacade pusherFacade;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private Logger log;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
   }

   public <T extends ContextualTask> T getInstance(final Class<T> clazz) {
      try {
         T t = clazz.getConstructor().newInstance();
         t.initialize(authenticatedUser.getCurrentUser(), daoContextSnapshotFactory.getInstance(), pusherFacade.getPusherClient(), new RequestDataKeeper(requestDataKeeper), constraintManager, configurationProducer.getEnvironment());

         return t;
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to instantiate a task: ", e);
      }

      return null;
   }
}
