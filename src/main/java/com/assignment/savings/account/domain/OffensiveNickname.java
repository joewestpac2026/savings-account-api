package com.assignment.savings.account.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "offensive_nicknames")
public class OffensiveNickname {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String value;

    protected OffensiveNickname() {
    }

    public OffensiveNickname(String value) {
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}
