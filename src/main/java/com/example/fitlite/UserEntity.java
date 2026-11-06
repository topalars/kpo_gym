package com.example.fitlite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "surname")
    private String surname;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @OneToOne(mappedBy = "userEntity")
    private UserSecurityEntity userSecurityDataEntity;

    public UserEntity(String name, String surname, UserRole role) {
        this.name = name;
        this.surname = surname;
        this.role = role;
    }
}
