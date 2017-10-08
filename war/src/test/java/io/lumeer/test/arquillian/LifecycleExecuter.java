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
package io.lumeer.test.arquillian;

import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;

import java.lang.reflect.Method;

public class LifecycleExecuter {

   public void executeBeforeDeploy(@Observes BeforeDeploy event, TestClass testClass) {
      execute(testClass.getMethods(io.lumeer.test.arquillian.annotation.BeforeDeploy.class));
   }

   public void executeAfterUnDeploy(@Observes AfterUnDeploy event, TestClass testClass) {
      execute(testClass.getMethods(io.lumeer.test.arquillian.annotation.AfterUnDeploy.class));
   }

   private void execute(Method[] methods) {
      if (methods == null) {
         return;
      }

      for (Method method : methods) {
         try {
            method.invoke(null);
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }
   }

}
