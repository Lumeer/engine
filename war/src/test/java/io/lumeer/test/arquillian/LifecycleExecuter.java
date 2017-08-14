/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
