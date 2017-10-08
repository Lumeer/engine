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
import io.lumeer.engine.api.push.PushMessage;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:kotrady.johnny@gmail.com">Jan Kotrady</a>
 */
public class ConstraintHint implements Hint {

   private DataDocument dataDocument;
   private String userName;
   private String collectionName;
   private List<Object> objects;
   private String wrongValue;
   private Date date;

   @Override
   public Hint call() throws Exception {
      date = new Date();
      if (dataDocument != null) {
         return contstraintCheck(dataDocument);
      }
      return null;
   }

   private ConstraintHint contstraintCheck(DataDocument dataDocument) {

      return null;
   }

   private void setWrongValue(String wrongValue){
      this.wrongValue = wrongValue;
   }



   @Override
   public boolean isApplicable() {
      return true;
   }

   @Override
   public boolean apply() {
      return true;
   }


   public void setDocument(DataDocument dataDocument) {
      this.dataDocument = dataDocument;
   }

   @Override
   public void setCollection(final String collectionName) {
      this.collectionName = collectionName;
   }

   @Override
   public void setUser(final String userName) {
      this.userName = userName;
   }

   @Override
   public Date getCreateDate() {
      return date;
   }

   @Override
   public PushMessage getMessage() {
      return new PushMessage("Hint message","Hint","You have wrong integer saved in: " + wrongValue );
   }
}
