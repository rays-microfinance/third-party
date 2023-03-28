package com.sahay.third.party.repo;

import com.sahay.third.party.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Integer> {
    Client findClientByUsername(String username);

    Optional<Client> findByUsername(String username);

    Optional<Client> findClientByClientReqType(String reqType);
}
