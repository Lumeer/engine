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
package io.lumeer.engine.hints;

import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
@SessionScoped
public class HintExecutor implements Serializable {
   int corePoolSize = 5;
   int maxPoolSize = 10;
   long keepAliveTime = 5000;

   @Inject
   ThreadPoolExecutor threadPool;

   public Future<Hint> runHintDetect(Hint hintToStart) {
      if (threadPool == null){
         threadPool = new ThreadPoolExecutor(
               corePoolSize,
               maxPoolSize,
               keepAliveTime,
               TimeUnit.MILLISECONDS,
               new LinkedBlockingQueue<Runnable>()
         );
      }
      Future<Hint> result = threadPool.submit(hintToStart);
      return result;
   }

   @PreDestroy
   public void delete(){
      threadPool.shutdownNow();
   }
}
