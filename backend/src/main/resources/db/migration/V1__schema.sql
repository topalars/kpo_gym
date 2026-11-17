CREATE TABLE mutation_lock (id integer PRIMARY KEY);
INSERT INTO mutation_lock VALUES (1);
CREATE TABLE users (
 id varchar(36) PRIMARY KEY, email varchar(150) NOT NULL UNIQUE,
 password_hash varchar(100) NOT NULL, name varchar(100) NOT NULL,
 role varchar(30) NOT NULL CHECK(role IN ('ROLE_USER','ROLE_COACH','ROLE_ADMIN','ROLE_SUPERADMIN')),
 phone varchar(30), active boolean NOT NULL
);
CREATE UNIQUE INDEX users_email_ci ON users(lower(email));
CREATE TABLE client_profiles (id varchar(36) PRIMARY KEY, user_id varchar(36) NOT NULL UNIQUE REFERENCES users(id), height integer CHECK(height BETWEEN 80 AND 250));
CREATE TABLE trainer_profiles (id varchar(36) PRIMARY KEY, user_id varchar(36) NOT NULL UNIQUE REFERENCES users(id), specialty varchar(150) NOT NULL);
CREATE TABLE memberships (id varchar(36) PRIMARY KEY, user_id varchar(36) NOT NULL UNIQUE REFERENCES users(id), plan varchar(50) NOT NULL CHECK(plan IN ('Без абонемента','Дневной','Безлимит','Персональный')), plan_until date);
CREATE TABLE training_sessions (
 id varchar(36) PRIMARY KEY, title varchar(100) NOT NULL, coach_id varchar(36) NOT NULL REFERENCES users(id),
 date date NOT NULL, time time NOT NULL, duration integer NOT NULL CHECK(duration BETWEEN 15 AND 180),
 capacity integer NOT NULL CHECK(capacity BETWEEN 1 AND 50), room varchar(80) NOT NULL
);
CREATE TABLE bookings (id varchar(36) PRIMARY KEY, training_id varchar(36) NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE, client_id varchar(36) NOT NULL REFERENCES users(id), UNIQUE(training_id,client_id));
CREATE INDEX bookings_client ON bookings(client_id);
CREATE TABLE progress_records (
 id varchar(36) PRIMARY KEY, user_id varchar(36) NOT NULL REFERENCES users(id), date date NOT NULL,
 weight numeric(7,2) NOT NULL CHECK(weight BETWEEN 25 AND 350), strength numeric(7,2) NOT NULL CHECK(strength BETWEEN 0 AND 500), UNIQUE(user_id,date)
);
CREATE TABLE events (
 id varchar(36) PRIMARY KEY, title varchar(120) NOT NULL, description varchar(1500) NOT NULL,
 date date NOT NULL, time time NOT NULL, type varchar(50) NOT NULL,
 capacity integer NOT NULL CHECK(capacity BETWEEN 1 AND 200), published boolean NOT NULL
);
CREATE TABLE event_registrations (id varchar(36) PRIMARY KEY, event_id varchar(36) NOT NULL REFERENCES events(id) ON DELETE CASCADE, client_id varchar(36) NOT NULL REFERENCES users(id), UNIQUE(event_id,client_id));
CREATE INDEX event_registrations_client ON event_registrations(client_id);
CREATE TABLE notifications (id varchar(36) PRIMARY KEY, user_id varchar(36) NOT NULL REFERENCES users(id), message varchar(1000) NOT NULL, created_at timestamp NOT NULL, is_read boolean NOT NULL);
CREATE INDEX notifications_user ON notifications(user_id,created_at DESC);
