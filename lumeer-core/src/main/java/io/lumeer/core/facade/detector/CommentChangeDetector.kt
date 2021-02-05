package io.lumeer.core.facade.detector

import io.lumeer.api.model.Collection
import io.lumeer.api.model.Document
import io.lumeer.api.model.NotificationType
import io.lumeer.api.model.ResourceComment
import io.lumeer.engine.api.event.DocumentCommentedEvent
import io.lumeer.engine.api.event.DocumentEvent

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
class CommentChangeDetector : AbstractPurposeChangeDetector() {

    override fun detectChanges(documentEvent: DocumentEvent?, collection: Collection?) {
    }

    override fun detectChanges(comment: ResourceComment, document: Document, collection: Collection) {
        super.detectChanges(comment, document, collection)

        delayedActionDao.scheduleActions(getDelayedActions(DocumentCommentedEvent(document, comment), collection, NotificationType.TASK_COMMENTED, nowPlus()))

        val mentionedUsers = getMentionedUsers(comment.comment)
        if (mentionedUsers.size > 0) {
            delayedActionDao.scheduleActions(getDelayedActions(DocumentCommentedEvent(document, comment), collection, NotificationType.TASK_MENTIONED, nowPlus(), mentionedUsers))
        }
    }

    private fun getMentionedUsers(commentText: String): Set<String> {
        val regEx = Regex("@([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})")
        val res = HashSet<String>()

        for (match in regEx.findAll(commentText).iterator()) {
            res.add(match.value.substring(1))
        }

        return res
    }
}