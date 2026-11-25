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
package io.lumeer.datafilters.datavalue

import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.util.valueMeetFulltexts

interface DataValue {
    /** String shown to the user. */
    fun format(): String

    /** Checks whether the value meets the condition with the given condition values. */
    fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean

    /** Checks whether the value contains all fulltexts. */
    fun meetFullTexts(fulltexts: List<String>): Boolean = valueMeetFulltexts(format(), fulltexts)
}
