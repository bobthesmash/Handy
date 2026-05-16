package cz.handy.feature.actions.notification

import android.app.Notification
import android.app.NotificationManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSnapshotPolicyTest {
    @Test
    fun ignoresQuietAndProgressAndService() {
        assertTrue(
            NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                NotificationManager.IMPORTANCE_MIN,
                null,
                ongoing = false,
            ),
        )
        assertTrue(
            NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                NotificationManager.IMPORTANCE_DEFAULT,
                Notification.CATEGORY_PROGRESS,
                ongoing = false,
            ),
        )
        assertTrue(
            NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                NotificationManager.IMPORTANCE_DEFAULT,
                Notification.CATEGORY_SERVICE,
                ongoing = false,
            ),
        )
    }

    @Test
    fun acceptsDefaultMessage() {
        assertFalse(
            NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                NotificationManager.IMPORTANCE_DEFAULT,
                Notification.CATEGORY_MESSAGE,
                ongoing = false,
            ),
        )
    }

    @Test
    fun ignoresOngoingUnlessHighEnough() {
        assertTrue(
            NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                NotificationManager.IMPORTANCE_LOW,
                null,
                ongoing = true,
            ),
        )
        assertFalse(
            NotificationSnapshotPolicy.shouldIgnoreForSnapshot(
                NotificationManager.IMPORTANCE_HIGH,
                null,
                ongoing = true,
            ),
        )
    }

    @Test
    fun replacePrefersHigherImportanceOrNewerSameTier() {
        assertTrue(
            NotificationSnapshotPolicy.shouldReplaceStored(
                newImportance = 4,
                newPostTime = 100L,
                prevImportance = 3,
                prevPostTime = 200L,
            ),
        )
        assertFalse(
            NotificationSnapshotPolicy.shouldReplaceStored(
                newImportance = 2,
                newPostTime = 300L,
                prevImportance = 4,
                prevPostTime = 100L,
            ),
        )
        assertTrue(
            NotificationSnapshotPolicy.shouldReplaceStored(
                newImportance = 3,
                newPostTime = 150L,
                prevImportance = 3,
                prevPostTime = 100L,
            ),
        )
    }

    @Test
    fun firstSnapshotAlwaysReplaces() {
        assertTrue(
            NotificationSnapshotPolicy.shouldReplaceStored(
                newImportance = 1,
                newPostTime = 1L,
                prevImportance = -1,
                prevPostTime = 0L,
            ),
        )
    }
}
