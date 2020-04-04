package io.lumeer.core.template;/*
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

import io.lumeer.api.model.Sequence;
import io.lumeer.core.facade.SequenceFacade;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Optional;

public class SequenceCreator extends WithIdCreator {

   private final SequenceFacade sequenceFacade;

   private SequenceCreator(final TemplateParser templateParser, final SequenceFacade sequenceFacade) {
      super(templateParser);
      this.sequenceFacade = sequenceFacade;
   }

   public static void createSequences(final TemplateParser templateParser, final SequenceFacade sequenceFacade) {
      final SequenceCreator creator = new SequenceCreator(templateParser, sequenceFacade);
      creator.createSequences();
   }

   private void createSequences() {
      final JSONArray sequences = (JSONArray) templateParser.template.get("sequences");

      if (sequences != null) {
         sequences.forEach(seqObj -> {
            var seqJson = (JSONObject) seqObj;
            String name = seqJson.get(Sequence.NAME).toString();
            int seq = ((Long) seqJson.get(Sequence.SEQ)).intValue();

            sequenceFacade.getNextSequenceNumber(name);
            Optional<Sequence> foundSeq = sequenceFacade.getAllSequences().stream().filter(s -> s.getName().equals(name)).findFirst();
            if (foundSeq.isPresent()) {
               var sequence = new Sequence(name, seq);
               sequence.setId(foundSeq.get().getId());
               sequenceFacade.updateSequence(foundSeq.get().getId(), sequence);
            }
         });
      }
   }
}
