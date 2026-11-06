package com.example.fitlite;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "user_security")
public class UserSecurityEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id")
    private UserEntity userEntity;

    public UserSecurityEntity(String password, String email, String phoneNumber) {
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
}
