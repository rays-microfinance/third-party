package com.sahay.third.party.repo;

import com.sahay.third.party.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

}
