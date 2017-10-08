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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataStorage;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RunWith(Arquillian.class)
public class SequenceFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private SequenceFacade sequenceFacade;

   @Test
   public void testSequences() {
      systemDataStorage.dropCollection("_sequences");
      sequenceFacade.init();

      assertThat(sequenceFacade.getNext("abcd")).isEqualTo(0);
      assertThat(sequenceFacade.getNext("abcd")).isEqualTo(1);
      assertThat(sequenceFacade.getNext("abcd")).isEqualTo(2);
      sequenceFacade.resetSequence("abcd");
      assertThat(sequenceFacade.getNext("abcd")).isEqualTo(1);
   }
}