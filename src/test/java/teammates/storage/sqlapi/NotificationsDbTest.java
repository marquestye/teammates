package teammates.storage.sqlapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.UUID;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import teammates.common.datatransfer.NotificationStyle;
import teammates.common.datatransfer.NotificationTargetUser;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.HibernateUtil;
import teammates.storage.sqlentity.Notification;
import teammates.test.BaseTestCase;

/**
 * SUT: {@code NotificationsDb}.
 */
public class NotificationsDbTest extends BaseTestCase {

    private NotificationsDb notificationsDb;

    private MockedStatic<HibernateUtil> mockHibernateUtil;

    @BeforeMethod
    public void setUpMethod() {
        mockHibernateUtil = mockStatic(HibernateUtil.class);
        notificationsDb = spy(NotificationsDb.class);
    }

    @AfterMethod
    public void teardownMethod() {
        mockHibernateUtil.close();
    }

    @Test
    public void testCreateNotification_notificationDoesNotExist_success()
            throws EntityAlreadyExistsException, InvalidParametersException {
        Notification newNotification = new Notification(Instant.parse("2011-01-01T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"), NotificationStyle.DANGER, NotificationTargetUser.GENERAL,
                "A deprecation note", "<p>Deprecation happens in three minutes</p>");

        notificationsDb.createNotification(newNotification);

        mockHibernateUtil.verify(() -> HibernateUtil.persist(newNotification));
    }

    @Test
    public void testCreateNotification_endTimeIsBeforeStartTime_throwsInvalidParametersException() {
        Notification invalidNotification = new Notification(Instant.parse("2011-02-01T00:00:00Z"),
                Instant.parse("2011-01-01T00:00:00Z"), NotificationStyle.DANGER, NotificationTargetUser.GENERAL,
                "A deprecation note", "<p>Deprecation happens in three minutes</p>");

        assertThrows(InvalidParametersException.class, () -> notificationsDb.createNotification(invalidNotification));
        mockHibernateUtil.verify(() -> HibernateUtil.persist(invalidNotification), never());
    }

    @Test
    public void testCreateNotification_emptyTitle_throwsInvalidParametersException() {
        Notification invalidNotification = new Notification(Instant.parse("2011-01-01T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"), NotificationStyle.DANGER, NotificationTargetUser.GENERAL,
                "", "<p>Deprecation happens in three minutes</p>");

        assertThrows(InvalidParametersException.class, () -> notificationsDb.createNotification(invalidNotification));
        mockHibernateUtil.verify(() -> HibernateUtil.persist(invalidNotification), never());
    }

    @Test
    public void testCreateNotification_emptyMessage_throwsInvalidParametersException() {
        Notification invalidNotification = new Notification(Instant.parse("2011-01-01T00:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z"), NotificationStyle.DANGER, NotificationTargetUser.GENERAL,
                "A deprecation note", "");

        assertThrows(InvalidParametersException.class, () -> notificationsDb.createNotification(invalidNotification));
        mockHibernateUtil.verify(() -> HibernateUtil.persist(invalidNotification), never());
    }

    @Test
    public void testGetNotification_success() {
        Notification notification = getTypicalNotificationWithId();
        mockHibernateUtil.when(() ->
                HibernateUtil.get(Notification.class, notification.getId())).thenReturn(notification);

        Notification actualNotification = notificationsDb.getNotification(notification.getId());

        mockHibernateUtil.verify(() -> HibernateUtil.get(Notification.class, notification.getId()));
        assertEquals(notification, actualNotification);
    }

    @Test
    public void testGetNotification_entityDoesNotExist() {
        UUID nonExistentId = UUID.fromString("00000000-0000-1000-0000-000000000000");
        mockHibernateUtil.when(() -> HibernateUtil.get(Notification.class, nonExistentId)).thenReturn(null);

        Notification actualNotification = notificationsDb.getNotification(nonExistentId);

        mockHibernateUtil.verify(() -> HibernateUtil.get(Notification.class, nonExistentId));
        assertNull(actualNotification);
    }

    @Test
    public void testDeleteNotification_entityExists_success() {
        Notification notification = getTypicalNotificationWithId();
        notificationsDb.deleteNotification(notification);
        mockHibernateUtil.verify(() -> HibernateUtil.remove(notification));
    }

    @Test
    public void testDeleteNotification_entityDoesNotExists_success() {
        notificationsDb.deleteNotification(null);
        mockHibernateUtil.verify(() -> HibernateUtil.remove(any()), never());
    }

    @Test
    public void testUpdateNotification_entityExists_success()
            throws EntityDoesNotExistException, InvalidParametersException {
        Notification notification = getTypicalNotificationWithId();
        doReturn(notification).when(notificationsDb).getNotification(notification.getId());

        notificationsDb.updateNotification(notification);

        mockHibernateUtil.verify(() -> HibernateUtil.merge(notification), times(1));
    }

    @Test
    public void testUpdateNotification_invalidParameters_throwsInvalidParametersException()
            throws EntityDoesNotExistException, InvalidParametersException {
        Notification notification = getTypicalNotificationWithId();
        notification.setTitle("");

        assertThrows(InvalidParametersException.class, () -> notificationsDb.updateNotification(notification));

        mockHibernateUtil.verify(() -> HibernateUtil.merge(notification), never());
    }

}
