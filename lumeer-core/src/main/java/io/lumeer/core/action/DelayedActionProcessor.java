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
package io.lumeer.core.action;

import io.lumeer.storage.api.dao.DelayedActionDao;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class DelayedActionProcessor {

   @Inject
   private DelayedActionDao delayedActionDao;

   @Schedule(hour = "*", minute = "*/2")
   public void process() {
      //System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ jedu jedu");
      //System.out.println(delayedActionDao.getActions());
      removeProcessedActions();
      removeProcessedActions();
      executeActions();
   }

   private void removeProcessedActions() {}

   private void resetTimeoutedActions() {}

   private void executeActions() {}
}
