package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt encoded!

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER; // По умолчанию — USER

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Card> cards;

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

}