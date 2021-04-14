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
package io.lumeer.core.task.executor;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.core.js.JsEngineFactory;
import io.lumeer.core.task.ContextualTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.bridge.LumeerBridge;
import io.lumeer.core.task.executor.operation.DocumentOperation;
import io.lumeer.core.task.executor.operation.OperationExecutor;
import io.lumeer.core.util.JsFunctionsParser;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class JsExecutor {

   private static final String MOMENT_JS_SIGNATURE = "/** MomentJs **/";
   private static final String HE_JS_SIGNATURE = "/** he.js **/";
   private static final String NUMBRO_JS_SIGNATURE = "/** numbro.js **/";

   private LumeerBridge lumeerBridge;
   private ContextualTask task;
   private boolean dryRun = false;
   private static final Engine engine = JsEngineFactory.getEngine();
   private static final String momentJsCode = JsFunctionsParser.getMomentJsCode();
   private static final String heJsCode = JsFunctionsParser.getHeJsCode();
   private static final String numbroJsCode = JsFunctionsParser.getNumbroJsCode();

   private String getJsLib() {
      return "function lumeer_numbro(locale, decimals, num) { numbro.setLanguage(locale); return numbro(num).formatCurrency({mantissa: decimals, thousandSeparated: true}); } "
            + "function lumeer_isEmpty(v) {\n"
            + "  return (v === null || v === undefined || v === '' || (Array.isArray(v) && (v.length === 0 || (v.length === 1 && lumeer_isEmpty(v[0])))) || (typeof v === 'object' && !!v && Object.keys(v).length === 0 && v.constructor === Object));\n"
            + "}\n";
   }

   public void execute(final Map<String, Object> bindings, final ContextualTask task, final String js) {
      this.task = task;
      lumeerBridge = new LumeerBridge(task);
      lumeerBridge.setDryRun(dryRun);

      Context context = Context
            .newBuilder("js")
            .engine(engine)
            .allowAllAccess(true)
            .build();
      context.initialize("js");
      context.getPolyglotBindings().putMember("lumeer", lumeerBridge);

      bindings.forEach((k, v) -> context.getBindings("js").putMember(k, v));

      Timer timer = new Timer(true);
      timer.schedule(new TimerTask() {
         @Override
         public void run() {
            context.close(true);
         }
      }, 3000);

      final String jsCode = getJsLib() +
            (js.contains(HE_JS_SIGNATURE) ? heJsCode : "") +
            (js.contains(NUMBRO_JS_SIGNATURE) ? numbroJsCode : "") +
            (js.contains(JsFunctionsParser.FORMAT_JS_DATE) || js.contains(JsFunctionsParser.PARSE_JS_DATE) || js.contains(MOMENT_JS_SIGNATURE) ? momentJsCode + ";\n" : "") + js;

      context.eval("js", jsCode);
   }

   public ChangesTracker commitOperations(final TaskExecutor taskExecutor) {
      final OperationExecutor operationExecutor = new OperationExecutor(taskExecutor, task, lumeerBridge.getOperations());
      return operationExecutor.call();
   }

   public ChangesTracker commitDryRunOperations(final TaskExecutor taskExecutor) {
      return lumeerBridge.commitDryRunOperations(taskExecutor);
   }

   public String getOperationsDescription() {
      return lumeerBridge.getOperationsDescription();
   }

   public void setErrorInAttribute(final Document document, final String attributeId, final TaskExecutor taskExecutor) {
      final OperationExecutor operationExecutor = new OperationExecutor(taskExecutor, task, Set.of(new DocumentOperation(document, attributeId, "ERR!")));
      operationExecutor.call();
   }

   public Exception getCause() {
      return lumeerBridge.getCause();
   }

   public void setDryRun(final boolean dryRun) {
      this.dryRun = dryRun;
   }
}
