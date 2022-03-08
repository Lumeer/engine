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
package io.lumeer.core.task;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.core.task.executor.ChangesTracker;

import java.io.Serializable;
import java.util.List;

/**
 * A task that can be processed.
 */
public interface Task extends Serializable {

   long MAX_CREATED_AND_DELETED_DOCUMENTS_AND_LINKS = 50L;
   long MAX_MESSAGES = 5L;
   long MAX_EMAILS = 3L;
   long MAX_ATTACHMENT_SIZE = 5L * 1024 * 1024;
   int MAX_EMAIL_RECIPIENTS = 100;
   int MAX_VIEW_DOCUMENTS = 1000;

   void setParent(final Task task);

   Task getParent();

   void process(final TaskExecutor executor, final ChangesTracker changesTracker);

   void propagateChanges(final List<Document> documents, final List<LinkInstance> links);

   void processChanges(final ChangesTracker changesTracker);

   int getRecursionDepth();
}
