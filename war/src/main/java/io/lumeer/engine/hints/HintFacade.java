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
