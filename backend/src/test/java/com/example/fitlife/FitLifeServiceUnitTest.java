package com.example.fitlife;

import com.example.fitlife.service.FitLifeService;
import com.example.fitlife.entity.*;
import com.example.fitlife.repository.*;
import com.example.fitlife.dto.Requests.*;
import com.example.fitlife.exception.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.*;
import java.time.*;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FitLifeServiceUnitTest {
    @Mock
    UserAccountRepository users;
    @Mock
    ClientProfileRepository clients;
    @Mock
    TrainerProfileRepository trainers;
    @Mock
    MembershipRepository memberships;
    @Mock
    TrainingSessionRepository sessions;
    @Mock
    BookingRepository bookings;
    @Mock
    ProgressRecordRepository progress;
    @Mock
    ClubEventRepository events;
    @Mock
    EventRegistrationRepository registrations;
    @Mock
    NotificationRepository notifications;
    @Mock
    PasswordEncoder passwords;
    @Mock
    JdbcTemplate jdbc;
    @InjectMocks
    FitLifeService service;
    UserAccount client, coach;
    TrainingSession slot;

    UserAccount user(String id, String role) {
        var u = new UserAccount();
        u.id = id;
        u.role = role;
        u.active = true;
        u.name = "Тестовый Пользователь";
        u.email = id + "@test.ru";
        u.passwordHash = "stored-hash";
        return u;
    }

    void signed(UserAccount u) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u.id, null, List.of()));
        lenient().when(users.findById(u.id)).thenReturn(Optional.of(u));
    }

    Booking booking(String training, String client) {
        var b = new Booking();
        b.trainingId = training;
        b.clientId = client;
        return b;
    }

    TrainingSession training(String id, String time) {
        var s = new TrainingSession();
        s.id = id;
        s.title = "Силовая";
        s.coachId = "coach";
        s.date = LocalDate.now().plusDays(10);
        s.time = LocalTime.parse(time);
        s.duration = 60;
        s.capacity = 2;
        s.room = "Зал1";
        return s;
    }

    void error(int status, Runnable action) {
        var e = assertThrows(ApiException.class, action::run);
        assertEquals(status, e.status);
    }

    @BeforeEach
    void arrange() {
        SecurityContextHolder.clearContext();
        client = user("client", "ROLE_USER");
        coach = user("coach", "ROLE_COACH");
        slot = training("slot", "10:00");
        signed(client);
        lenient().when(users.findById("coach")).thenReturn(Optional.of(coach));
        lenient().when(sessions.findById("slot")).thenReturn(Optional.of(slot));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void U01_authenticateValidPassword() {
        when(users.findByEmailIgnoreCase("client@test.ru")).thenReturn(Optional.of(client));
        when(passwords.matches("right", "stored-hash")).thenReturn(true);
        var actual = service.authenticate(new Login(" client@test.ru ", "right"));
        assertSame(client, actual);
        verify(passwords).matches("right", "stored-hash");
    }

    @Test
    void U02_unknownEmailRejected() {
        error(401, () -> service.authenticate(new Login("none@test.ru", "right")));
        verifyNoInteractions(passwords);
    }

    @Test
    void U03_wrongPasswordRejected() {
        when(users.findByEmailIgnoreCase(client.email)).thenReturn(Optional.of(client));
        error(401, () -> service.authenticate(new Login(client.email, "wrong")));
    }

    @Test
    void U04_disabledAccountRejected() {
        client.active = false;
        when(users.findByEmailIgnoreCase(client.email)).thenReturn(Optional.of(client));
        error(401, () -> service.authenticate(new Login(client.email, "right")));
        verifyNoInteractions(passwords);
    }

    @Test
    void U05_guestRejected() {
        SecurityContextHolder.clearContext();
        error(401, () -> service.me());
    }

    @Test
    void U06_removedAccountRejected() {
        when(users.findById("client")).thenReturn(Optional.empty());
        error(401, () -> service.me());
    }

    @Test
    void U07_disabledSessionRejected() {
        client.active = false;
        error(401, () -> service.me());
    }

    @ParameterizedTest(name = "U08 admin({0})={1}")
    @CsvSource({"ROLE_ADMIN,true", "ROLE_SUPERADMIN,true", "ROLE_USER,false", "ROLE_COACH,false"})
    void U08_administratorRoles(String role, boolean expected) {
        assertEquals(expected, service.admin(user("actor", role)));
    }

    @Test
    void U09_bookingSavesClientAndNotifications() {
        service.book("slot", false);
        var saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookings).save(saved.capture());
        assertEquals("slot", saved.getValue().trainingId);
        assertEquals("client", saved.getValue().clientId);
        verify(notifications, times(2)).save(any());
        verify(jdbc).queryForObject("SELECT id FROM mutation_lock WHERE id=1 FOR UPDATE", Integer.class);
    }

    @Test
    void U10_missingTrainingRejected() {
        error(404, () -> service.book("missing", false));
        verify(bookings, never()).save(any());
    }

    @Test
    void U11_pastTrainingRejected() {
        slot.date = LocalDate.now().minusDays(1);
        error(400, () -> service.book("slot", false));
        verify(bookings, never()).save(any());
    }

    @ParameterizedTest(name = "U12 role {0} cannot book")
    @ValueSource(strings = {"ROLE_COACH", "ROLE_ADMIN", "ROLE_SUPERADMIN"})
    void U12_nonClientCannotBook(String role) {
        signed(user("actor", role));
        error(403, () -> service.book("slot", false));
        verify(bookings, never()).save(any());
    }

    @Test
    void U13_duplicateBookingRejected() {
        when(bookings.findAll()).thenReturn(List.of(booking("slot", "client")));
        error(409, () -> service.book("slot", false));
        verify(bookings, never()).save(any());
    }

    @Test
    void U14_inactiveCoachRejected() {
        coach.active = false;
        error(409, () -> service.book("slot", false));
        verify(bookings, never()).save(any());
    }

    @Test
    void U15_fullTrainingRejected() {
        when(bookings.findAll()).thenReturn(List.of(booking("slot", "other1"), booking("slot", "other2")));
        error(409, () -> service.book("slot", false));
        verify(bookings, never()).save(any());
    }

    @Test
    void U16_clientOverlapRejected() {
        var other = training("other", "10:30");
        when(bookings.findAll()).thenReturn(List.of(booking("other", "client")));
        when(sessions.findAll()).thenReturn(List.of(other));
        error(409, () -> service.book("slot", false));
    }

    @Test
    void U17_adjacentTrainingsAllowed() {
        var other = training("other", "11:00");
        when(bookings.findAll()).thenReturn(List.of(booking("other", "client")));
        when(sessions.findAll()).thenReturn(List.of(other));
        assertDoesNotThrow(() -> service.book("slot", false));
        verify(bookings).save(any());
    }

    @Test
    void U18_cancelExistingBooking() {
        var b = booking("slot", "client");
        when(bookings.findAll()).thenReturn(List.of(b));
        service.book("slot", true);
        verify(bookings).delete(b);
        verify(bookings, never()).save(any());
        verify(notifications, times(2)).save(any());
    }

    @Test
    void U19_cancelMissingBookingRejected() {
        error(404, () -> service.book("slot", true));
        verify(bookings, never()).delete(any());
    }

    @Test
    void U20_createProgress() {
        var r = new Progress(LocalDate.now(), new BigDecimal("80"), new BigDecimal("60"));
        service.saveProgress(r);
        var c = ArgumentCaptor.forClass(ProgressRecord.class);
        verify(progress).save(c.capture());
        assertEquals("client", c.getValue().userId);
        assertEquals(r.weight(), c.getValue().weight);
        assertEquals(r.strength(), c.getValue().strength);
    }

    @Test
    void U21_updateProgressKeepsId() {
        var p = new ProgressRecord();
        p.userId = "client";
        p.date = LocalDate.now();
        when(progress.findAll()).thenReturn(List.of(p));
        String old = p.id;
        service.saveProgress(new Progress(p.date, new BigDecimal("81"), new BigDecimal("65")));
        verify(progress).save(p);
        assertEquals(old, p.id);
        assertEquals(new BigDecimal("81"), p.weight);
    }

    @Test
    void U22_coachCannotWriteClientProgress() {
        signed(coach);
        error(403, () -> service.saveProgress(new Progress(LocalDate.now(), BigDecimal.TEN, BigDecimal.ONE)));
        verify(progress, never()).save(any());
    }

    @Test
    void U23_publicViewDoesNotExposeHash() {
        var map = service.userView(client);
        assertEquals(client.id, map.get("id"));
        assertFalse(map.containsKey("passwordHash"));
        assertFalse(map.containsKey("password"));
        assertEquals("", map.get("phone"));
    }

    @Test
    void U24_notificationsPrivateAndSorted() {
        var old = new Notification();
        old.userId = "client";
        old.createdAt = LocalDateTime.now().minusDays(1);
        var recent = new Notification();
        recent.userId = "client";
        recent.createdAt = LocalDateTime.now();
        var чужое = new Notification();
        чужое.userId = "other";
        чужое.createdAt = recent.createdAt;
        when(notifications.findAll()).thenReturn(List.of(old, чужое, recent));
        assertEquals(List.of(recent, old), service.ownNotifications());
    }

    @Test
    void U25_cannotReadAnotherNotification() {
        var n = new Notification();
        n.userId = "other";
        when(notifications.findById(n.id)).thenReturn(Optional.of(n));
        error(403, () -> service.readNotification(n.id));
        verify(notifications, never()).save(any());
    }

    @Test
    void U26_markOwnNotificationRead() {
        var n = new Notification();
        n.userId = "client";
        when(notifications.findById(n.id)).thenReturn(Optional.of(n));
        service.readNotification(n.id);
        assertTrue(n.read);
        verify(notifications).save(n);
    }

    @Test
    void U27_registerClientCreatesRelatedRecords() {
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwords.encode("Goodpass123!")).thenReturn("bcrypt-test");
        var u = service.register(new Register(" Новый Клиент ", "NEW@Test.ru", "Goodpass123!"));
        assertEquals("ROLE_USER", u.role);
        assertEquals("new@test.ru", u.email);
        assertEquals("Новый Клиент", u.name);
        assertEquals("bcrypt-test", u.passwordHash);
        verify(clients).save(any());
        verify(memberships).save(any());
    }

    @Test
    void U28_registrationDuplicateRejected() {
        when(users.findByEmailIgnoreCase("client@test.ru")).thenReturn(Optional.of(client));
        error(409, () -> service.register(new Register("Клиент", "client@test.ru", "Goodpass123!")));
        verify(users, never()).save(any());
    }

    @ParameterizedTest(name = "U29 short password: {0}")
    @ValueSource(strings = {"", "short", "1234567"})
    void U29_shortPasswordRejected(String password) {
        error(400, () -> service.register(new Register("Клиент", "new@test.ru", password)));
        verify(users, never()).save(any());
    }

    @Test
    void U30_multibytePasswordTooLongRejected() {
        error(400, () -> service.register(new Register("Клиент", "new@test.ru", "я".repeat(37))));
        verify(users, never()).save(any());
    }

    @Test
    void U31_invalidPlanRejected() {
        signed(user("admin", "ROLE_ADMIN"));
        error(400, () -> service.savePlan(new Plan("unknown", null), "client"));
        verify(memberships, never()).save(any());
    }

    @Test
    void U32_planRequiresExpiration() {
        signed(user("admin", "ROLE_ADMIN"));
        error(400, () -> service.savePlan(new Plan("Безлимит", null), "client"));
        verify(memberships, never()).save(any());
    }

    @Test
    void U33_planSavedAndNotified() {
        signed(user("admin", "ROLE_ADMIN"));
        var end = LocalDate.now().plusMonths(1);
        service.savePlan(new Plan("Безлимит", end), "client");
        var c = ArgumentCaptor.forClass(Membership.class);
        verify(memberships).save(c.capture());
        assertEquals("client", c.getValue().userId);
        assertEquals(end, c.getValue().planUntil);
        verify(notifications).save(any());
    }

    @Test
    void U34_noPlanClearsExpiration() {
        signed(user("admin", "ROLE_ADMIN"));
        var m = new Membership();
        m.userId = "client";
        m.planUntil = LocalDate.now();
        when(memberships.findAll()).thenReturn(List.of(m));
        service.savePlan(new Plan("Без абонемента", LocalDate.now()), "client");
        assertNull(m.planUntil);
        verify(memberships).save(m);
    }
}
