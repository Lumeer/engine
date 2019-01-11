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
package io.lumeer.core.util;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class PolyglotExecutor {

   public static class Helper {

      public Callable<String> call = () -> "hello caller";

      public String callOnMe(int value, String str, PolyglotExecutor a) {
         return str + (a.getA() + value);
      }
   }

   public static class Document {

   }

   public static class Collection {

   }

   public static class LinkType {

   }

   public static class LumeerBridge {
      void setDocumentAttribute(Document d, String attrId, Value value) {

      }

      List<Document> getLinkedDocuments(Document d, String linkTypeId) {
         return null;
      }

      List<Value> getDocumentAttribute(Document d, String attrId) {
         return null;
      }

      List<Value> getDocumentAttribute(List<Document> docs, String attrId) {
         return null;
      }
   }

    /*
    var newDocument;


var lumeer = Polyglot.import('lumeer');
  lumeer.setDocumentAttribute(
      newDocument, 'a4',
      lumeer.getDocumentAttribute(
        lumeer.getLinkedDocuments(
           newDocument, '5b58a3fd2ab79c005fc0e70c'),
        'B').reduce(function(x, y) {return x + y;}))

     */

   private int a = 4;

   public int getA() {
      return a;
   }

   public static void main( String[] args )
   {
      Context context = Context.create("js");
      context.initialize("js");
      context.getPolyglotBindings().putMember("helper", new Helper());
      context.getPolyglotBindings().putMember("app", new PolyglotExecutor());
      //context.getPolyglotBindings().putMember("newDoc", Value.asValue(42));
      context.getBindings("js").putMember("newDoc", 42);
      context.eval("js",
            "var helper = Polyglot.import('helper');"
                  + "var newDoc;"
                  + "var app = Polyglot.import('app');"
                  + "print(helper.callOnMe(3, 'Hello Javascript '+newDoc, app));"
      );
   }


}
