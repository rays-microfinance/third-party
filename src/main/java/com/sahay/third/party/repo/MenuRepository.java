package com.sahay.third.party.repo;

import com.sahay.third.party.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu , Integer> {

    Optional<Menu> findMenuByName(String name);
}
