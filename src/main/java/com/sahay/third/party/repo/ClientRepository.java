package com.sahay.third.party.repo;

import com.sahay.third.party.model.Client;
import com.sahay.third.party.object.UserResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    Client findClientByUsername(String username);

    Client findClientByEmail(String email);

    Client findClientByPhoneNumber(String phoneNumber);

    Optional<Client> findClientByClientReqType(String reqType);

//    @EntityGraph(attributePaths = "role")
//    List<Client> findAll();

//    @Query(value = "SELECT client.id , client.username , client.email , client.phone_number, role.name as role  FROM Client client JOIN Role role ON role.id = client.role_id order by client.id desc", nativeQuery = true)
//    List<UserResponse> getAll();
}
