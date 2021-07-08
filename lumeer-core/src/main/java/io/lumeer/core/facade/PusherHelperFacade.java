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
package io.lumeer.core.facade;

import io.lumeer.core.action.DelayedActionProcessor;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.PusherClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PusherHelperFacade {

   private PusherClient pusherClient = null;

   @Inject
   private DelayedActionProcessor delayedActionProcessor;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @PostConstruct
   public void init() {
      pusherClient = PusherClient.getInstance(configurationProducer);
      delayedActionProcessor.setPusherClient(pusherClient);
   }

   public PusherClient getPusherClient() {
      return pusherClient;
   }
}
