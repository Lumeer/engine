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

import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;

import io.sentry.Sentry;
import io.sentry.event.UserBuilder;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SentryFacade {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   private static String SENTRY_DSN = null;

   @PostConstruct
   public void init() {
      SENTRY_DSN = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.SENTRY_DSN)).orElse("");

      if (isEnabled()) {
         Sentry.init(SENTRY_DSN);
      }
   }

   public void reportError(final Exception e) {
      if (isEnabled() && authenticatedUser != null) {
         final String userEmail = authenticatedUser.getUserEmail();

         if (userEmail != null && !"".equals(userEmail)) {
            Sentry.getContext().setUser(new UserBuilder().setEmail(userEmail).build());
         }

         Sentry.capture(e);
      }
   }

   private boolean isEnabled() {
      return SENTRY_DSN != null && !"".equals(SENTRY_DSN);
   }
}
