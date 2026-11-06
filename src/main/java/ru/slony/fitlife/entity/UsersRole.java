package ru.slony.fitlife.entity;

// Роли пользователей приложения. Префикс ROLE_ - соглашение Spring Security: authority
// сравнивается буквально (hasRole("ADMIN") сама добавит ROLE_, hasAuthority("ROLE_ADMIN") - нет).
public enum UsersRole {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_COACH,
    ROLE_SUPERADMIN
}
