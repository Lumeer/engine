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

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.push.PushService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
@SessionScoped
public class HintFacade implements Serializable {

   @Inject
   HintExecutor hintEx;

   @Inject
   UserFacade userFacade;

   @Inject
   PushService pushService;

   private List<Future<Hint>> hintsList = new ArrayList<>();
   private List<Hint> activeHints = new ArrayList<>();


   public void getHint() throws ExecutionException, InterruptedException {
      Hint hint = null;
      if (!hintsList.isEmpty()) {
         for (Future<Hint> future : hintsList) {
            if (future.isDone()) {
               hint = future.get();
               if (hint != null) {
                  hintsList.remove(future);
                  activeHints.add(hint);
                  pushService.publishMessageToCurrentUser("", hint.getMessage());
                  break;
               } else {
                  hintsList.remove(future);
               }
            }
         }
      }
   }

   public void removeOldHints(){

   }

   public String getHintText() throws ExecutionException, InterruptedException {
      Hint hint = null;
      if (!hintsList.isEmpty()) {
         for (Future<Hint> future : hintsList) {
            if (future.isDone()) {
               hint = future.get();
               if (hint != null) {
                  hintsList.remove(future);
                  activeHints.add(hint);
                  return hint.getMessage().getMessage();
               }
            }
         }
      }
      return "No hint";
   }

   public void runHint(String hintName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
      Hint hint = (Hint) Class.forName("io.lumeer.engine.hints." + hintName).newInstance();
      hint.setUser(userFacade.getUserName());
      hintsList.add(hintEx.runHintDetect(hint));
   }

   public void runHint(String hintName, DataDocument dataDocument) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
      Hint hint = (Hint) Class.forName("io.lumeer.engine.hints."  + hintName).newInstance();
      hint.setUser(userFacade.getUserName());
      hint.setDocument(dataDocument);
      hintsList.add(hintEx.runHintDetect(hint));
   }

   public void runHint(String hintName, String collectionName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
      Hint hint = (Hint) Class.forName("io.lumeer.engine.hints."  + hintName).newInstance();
      hint.setUser(userFacade.getUserName());
      hint.setCollection(collectionName);
      hintsList.add(hintEx.runHintDetect(hint));
   }

   public void runHint(String hintName, String collectionName, DataDocument dataDocument) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
      Hint hint = (Hint) Class.forName("io.lumeer.engine.hints."  + hintName).newInstance();
      hint.setUser(userFacade.getUserName());
      hint.setCollection(collectionName);
      hint.setDocument(dataDocument);
      hintsList.add(hintEx.runHintDetect(hint));
   }



   public boolean haveHint() {
      if (hintsList.isEmpty()) {
         return false;
      }
      return true;
   }

   public void clearHints() {
      hintsList.clear();
   }

   public void clearOldHints() {
      //TODO
   }
}
