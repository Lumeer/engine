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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.Project;
import io.lumeer.api.model.Sequence;

import java.util.List;

public interface SequenceDao {

   String INDEX_NAME = "name";

   void createSequencesRepository(final Project project);
   void deleteSequencesRepository(final Project project);

   List<Sequence> getAllSequences();
   Sequence getSequence(final String name);
   Sequence updateSequence(final String id, final Sequence sequence);
   void deleteSequence(final String id);

   int getNextSequenceNo(final String indexName);
   void resetSequence(final String indexName);

}
