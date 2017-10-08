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
package io.lumeer.engine.hints;

import java.io.Serializable;
import java.util.concurrent.Future;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
@RequestScoped
public class HintExecutor implements Serializable {

   private int corePoolSize = 5;
   private int maxPoolSize = 10;
   private long keepAliveTime = 5000;

   @Inject
   private ManagedExecutorService executorService;

   public Future<Hint> runHintDetect(final Hint hintToStart) {
      return executorService.submit(hintToStart);
   }
}
