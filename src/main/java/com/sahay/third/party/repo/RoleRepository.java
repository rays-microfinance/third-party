package com.sahay.third.party.repo;

import com.sahay.third.party.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RoleRepository extends JpaRepository<Role , Integer> {

    Optional<Role> findRoleByName(String name);

    Optional<Role> findRoleByRoleCode(String code);
}
