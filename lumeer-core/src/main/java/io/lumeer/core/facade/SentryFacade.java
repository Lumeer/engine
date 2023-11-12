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
import io.sentry.protocol.User;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

         if (StringUtils.isNotEmpty(userEmail)) {
            final User u = new User();
            u.setEmail(userEmail);
            Sentry.setUser(u);
         }

         Sentry.captureException(e);
      }
   }

   private boolean isEnabled() {
      return StringUtils.isNotEmpty(SENTRY_DSN);
   }
}
