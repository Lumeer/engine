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

import io.lumeer.api.model.User;
import io.lumeer.engine.IntegrationTestBase;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;

@ExtendWith(ArquillianExtension.class)
public class MailerLiteFacadeIT extends IntegrationTestBase {

   @Inject
   private MailerService mailerService;

   @Test
   @Disabled("It does not make sense to communicate with MailerLite API once it was verified")
   public void testMailerLite() {
      final User u = new User("123", "Pepin", "aturing@lumeer.io", null, null, false, null, true, false, null, null, null);
      mailerService.setUserSubscription(u, false);
   }
}
