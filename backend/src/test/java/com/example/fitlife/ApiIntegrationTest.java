package com.example.fitlife;

import com.fasterxml.jackson.databind.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = {"fitlife.admin-email=admin@test.ru", "fitlife.admin-password=Testpass123!"})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    static EmbeddedPostgres pg;

    static {
        try {
            pg = EmbeddedPostgres.builder().setPort(0).start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> pg.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "");
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    com.example.fitlife.service.FitLifeService service;
    MockHttpSession admin;

    @BeforeEach
    void setup() throws Exception {
        jdbc.execute("TRUNCATE notifications,event_registrations,events,progress_records,bookings,training_sessions,memberships,client_profiles,trainer_profiles,users CASCADE");
        service.bootstrap("admin@test.ru", "Testpass123!");
        admin = login("admin@test.ru");
    }

    @AfterAll
    static void stop() throws Exception {
        pg.close();
    }

    MvcResult call(String method, String path, MockHttpSession session, Object data) throws Exception {
        var b = switch (method) {
            case "POST" -> post(path);
            case "PATCH" -> patch(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> get(path);
        };
        if (session != null) b.session(session);
        if (!method.equals("GET")) b.with(csrf());
        if (data != null) b.contentType("application/json").content(json.writeValueAsBytes(data));
        return mvc.perform(b).andReturn();
    }

    JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    void status(int expected, MvcResult r) throws Exception {
        assertEquals(expected, r.getResponse().getStatus(), r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    MockHttpSession login(String email) throws Exception {
        var r = call("POST", "/api/auth/login", null, Map.of("email", email, "password", "Testpass123!"));
        status(200, r);
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    MockHttpSession client(String email) throws Exception {
        var r = call("POST", "/api/auth/register", null, Map.of("name", "Тест Клиент", "email", email, "password", "Testpass123!"));
        status(200, r);
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    JsonNode snapshot(MockHttpSession s) throws Exception {
        var r = call("GET", "/api/snapshot", s, null);
        status(200, r);
        return body(r);
    }

    String coach(String email) throws Exception {
        status(200, call("POST", "/api/trainers", admin, Map.of("name", "Тест Тренер", "email", email, "password", "Testpass123!", "specialty", "Силовые", "active", true)));
        return jdbc.queryForObject("SELECT id FROM users WHERE email=?", String.class, email);
    }

    Map<String, Object> training(String title, String coach, String time, int capacity, String room) {
        return new HashMap<>(Map.of("title", title, "coachId", coach, "date", LocalDate.now().plusDays(2).toString(), "time", time, "duration", 60, "capacity", capacity, "room", room));
    }

    String createTraining(Map<String, Object> data) throws Exception {
        status(200, call("POST", "/api/trainings", admin, data));
        return jdbc.queryForObject("SELECT id FROM training_sessions WHERE title=?", String.class, data.get("title"));
    }

    @Test
    void loginRegistrationAndRoleInjection() throws Exception {
        var c = client("client@test.ru");
        assertEquals("ROLE_USER", snapshot(c).get("user").get("role").asText());
        status(409, call("POST", "/api/auth/register", null, Map.of("name", "Дубликат", "email", "CLIENT@test.ru", "password", "Testpass123!")));
        status(400, call("POST", "/api/auth/register", null, Map.of("name", "Новый", "email", "bad-email", "password", "short")));
        status(400, call("POST", "/api/auth/register", null, Map.of("name", "Новый", "email", "hack@test.ru", "password", "Testpass123!", "role", "ROLE_ADMIN")));
        status(401, call("POST", "/api/auth/login", null, Map.of("email", "client@test.ru", "password", "wrong")));
        assertFalse(snapshot(c).toString().contains("passwordHash"));
        status(403, call("GET", "/api/statistics", c, null));
        status(401, call("GET", "/api/snapshot", null, null));
    }

    @Test
    void realCsrfSessionRotationAndLogout() throws Exception {
        var token = mvc.perform(get("/api/auth/csrf")).andReturn();
        var session = (MockHttpSession) token.getRequest().getSession();
        var t = body(token);
        String oldId = session.getId();
        var invalid = mvc.perform(post("/api/auth/login").session(session).contentType("application/json").content("{\"email\":\"admin@test.ru\",\"password\":\"Testpass123!\"}")).andReturn();
        status(403, invalid);
        var r = mvc.perform(post("/api/auth/login").session(session).header(t.get("headerName").asText(), t.get("token").asText()).contentType("application/json").content("{\"email\":\"admin@test.ru\",\"password\":\"Testpass123!\"}")).andReturn();
        status(200, r);
        assertNotEquals(oldId, session.getId());
        status(200, call("GET", "/api/auth/session", session, null));
        status(204, call("POST", "/api/auth/logout", session, null));
        status(401, call("GET", "/api/snapshot", null, null));
    }

    @Test
    void profileProgressBoundariesAndInvalidDate() throws Exception {
        var c = client("client@test.ru");
        var other = client("other@test.ru");
        for (int h : new int[]{79, 80, 250, 251})
            status(h < 80 || h > 250 ? 400 : 200, call("PATCH", "/api/me", c, Map.of("name", "Новое Имя", "height", h, "phone", "123")));
        for (double w : new double[]{24.9, 25, 350, 350.1})
            status(w < 25 || w > 350 ? 400 : 200, call("PUT", "/api/progress", c, Map.of("date", LocalDate.now().toString(), "weight", w, "strength", 60)));
        status(400, call("PUT", "/api/progress", c, Map.of("date", "2026-02-30", "weight", 80, "strength", 60)));
        status(400, call("PUT", "/api/progress", c, Map.of("date", LocalDate.now().plusDays(1).toString(), "weight", 80, "strength", 60)));
        assertEquals(1, snapshot(c).get("progress").size());
        assertEquals(0, snapshot(other).get("progress").size());
    }

    @Test
    void coachOwnershipAndRoomConflicts() throws Exception {
        var a = coach("coach@test.ru");
        var b = coach("coach2@test.ru");
        var session = login("coach@test.ru");
        var id = createTraining(training("Занятие", b, "10:00", 5, "Зал1"));
        status(403, call("PATCH", "/api/trainings/" + id, session, training("Изменённое", a, "12:00", 5, "Зал2")));
        assertEquals(0, snapshot(session).get("slots").size());
        status(409, call("POST", "/api/trainings", admin, training("Зал занят", a, "10:30", 5, "Зал1")));
        status(409, call("POST", "/api/trainings", admin, training("Тренер занят", b, "10:30", 5, "Зал2")));
        status(403, call("POST", "/api/trainings", client("client@test.ru"), training("Нет доступа", a, "12:00", 5, "Зал2")));
    }

    @Test
    void bookingCancellationAndMovingParticipants() throws Exception {
        var a = coach("coach@test.ru");
        var b = coach("coach2@test.ru");
        var c = client("client@test.ru");
        var first = createTraining(training("Первая", a, "10:00", 3, "Зал1"));
        var second = createTraining(training("Вторая", a, "18:00", 3, "Зал1"));
        status(200, call("POST", "/api/bookings", c, Map.of("trainingId", first)));
        status(409, call("POST", "/api/bookings", c, Map.of("trainingId", first)));
        status(200, call("POST", "/api/bookings", c, Map.of("trainingId", second)));
        status(409, call("PATCH", "/api/trainings/" + second, admin, training("Вторая", b, "10:30", 3, "Зал2")));
        assertEquals("18:00:00", jdbc.queryForObject("SELECT time::text FROM training_sessions WHERE id=?", String.class, second));
        status(200, call("DELETE", "/api/bookings/" + first, c, null));
        status(404, call("DELETE", "/api/bookings/" + first, c, null));
        status(200, call("PATCH", "/api/trainings/" + second, admin, training("Вторая", b, "10:30", 3, "Зал2")));
        assertTrue(snapshot(c).get("notifications").size() > 0);
    }

    @Test
    void concurrentLastPlaceAndDifferentOverlappingSlots() throws Exception {
        var a = coach("coach@test.ru");
        var b = coach("coach2@test.ru");
        var c1 = client("c1@test.ru");
        var c2 = client("c2@test.ru");
        var id = createTraining(training("Одно место", a, "10:00", 1, "Зал1"));
        assertEquals(List.of(200, 409), race(c1, id, c2, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM bookings WHERE training_id=?", Integer.class, id));
        var c3 = client("c3@test.ru");
        var id2 = createTraining(training("Вторая", a, "15:00", 3, "Зал1"));
        var id3 = createTraining(training("Третья", b, "15:30", 3, "Зал2"));
        assertEquals(List.of(200, 409), race(c3, id2, c3, id3));
    }

    List<Integer> race(MockHttpSession a, String aid, MockHttpSession b, String bid) throws Exception {
        var gate = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var f1 = pool.submit(() -> {
                gate.await();
                return call("POST", "/api/bookings", a, Map.of("trainingId", aid)).getResponse().getStatus();
            });
            var f2 = pool.submit(() -> {
                gate.await();
                return call("POST", "/api/bookings", b, Map.of("trainingId", bid)).getResponse().getStatus();
            });
            gate.countDown();
            var result = new ArrayList<>(List.of(f1.get(20, TimeUnit.SECONDS), f2.get(20, TimeUnit.SECONDS)));
            Collections.sort(result);
            return result;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void membershipAtomicValidationAndSuspension() throws Exception {
        var c = client("client@test.ru");
        var id = snapshot(c).get("user").get("id").asText();
        status(200, call("PUT", "/api/memberships/" + id, admin, Map.of("plan", "Безлимит", "planUntil", LocalDate.now().plusMonths(1).toString())));
        assertEquals("Безлимит", snapshot(c).get("user").get("plan").asText());
        status(400, call("PATCH", "/api/clients/" + id, admin, Map.of("name", "Изменённое", "active", false, "plan", "wrong")));
        assertTrue(snapshot(c).get("user").get("active").asBoolean());
        status(200, call("PATCH", "/api/clients/" + id, admin, Map.of("name", "Клиент", "active", false, "plan", "Без абонемента")));
        status(401, call("GET", "/api/snapshot", c, null));
        status(401, call("POST", "/api/auth/login", null, Map.of("email", "client@test.ru", "password", "Testpass123!")));
    }

    @Test
    void eventsPublicationCapacityAndPrivateNotifications() throws Exception {
        var c = client("client@test.ru");
        var other = client("other@test.ru");
        var data = new HashMap<String, Object>(Map.of("title", "Событие", "description", "Описание", "date", LocalDate.now().plusDays(3).toString(), "time", "12:00", "type", "Мастер-класс", "capacity", 1, "published", false));
        status(200, call("POST", "/api/events", admin, data));
        var id = jdbc.queryForObject("SELECT id FROM events", String.class);
        assertEquals(0, snapshot(c).get("events").size());
        status(404, call("POST", "/api/events/" + id + "/registration", c, null));
        data.put("published", true);
        status(200, call("PATCH", "/api/events/" + id, admin, data));
        status(200, call("POST", "/api/events/" + id + "/registration", c, null));
        status(409, call("POST", "/api/events/" + id + "/registration", other, null));
        var notice = snapshot(c).get("notifications").get(0).get("id").asText();
        status(403, call("PATCH", "/api/notifications/" + notice + "/read", other, null));
        status(200, call("PATCH", "/api/notifications/" + notice + "/read", c, null));
        status(200, call("DELETE", "/api/events/" + id + "/registration", c, null));
        status(200, call("POST", "/api/events/" + id + "/registration", other, null));
        status(200, call("DELETE", "/api/events/" + id, admin, null));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM event_registrations", Integer.class));
    }
}


