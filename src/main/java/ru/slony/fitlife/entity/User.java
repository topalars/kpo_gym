package ru.slony.fitlife.entity;

// Пока обычный абстрактный класс, а не JPA-сущность: нет @Entity, @Id и геттеров/сеттеров,
// поэтому таблицу в БД он не создаёт.
public abstract class User {

    private Integer id;
    private String name;
    private String email;
    private String password;
    // роль хранится как перечисление (ROLE_ADMIN, ROLE_USER, ...)
    private UsersRole role;

}
