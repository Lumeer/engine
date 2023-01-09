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
package io.lumeer.core.adapter

import io.lumeer.api.model.InformationRecord
import io.lumeer.storage.api.dao.InformationStoreDao
import java.time.ZonedDateTime

class InformationStoreAdapter(private val informationStoreDao: InformationStoreDao) {

   fun addInformation(informationRecord: InformationRecord, userId: String): InformationRecord {
      informationStoreDao.deleteStaleInformation()

      val rec = InformationRecord(null, userId, ZonedDateTime.now(), informationRecord.source, informationRecord.target, informationRecord.data)

      return informationStoreDao.addInformation(rec)
   }

   fun getInformation(id: String, userId: String) = informationStoreDao.findInformation(id, userId)
}