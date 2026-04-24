package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    // Найти карту по ID и владельцу (для проверки прав доступа)
    Optional<Card> findByIdAndOwner(Long id, User owner);

    // Все карты пользователя с пагинацией
    Page<Card> findAllByOwner(User owner, Pageable pageable);

    // Все карты системы (для админа)
    Page<Card> findAll(Pageable pageable);

}