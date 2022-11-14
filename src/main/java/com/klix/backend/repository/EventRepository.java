package com.klix.backend.repository;


import com.klix.backend.model.Event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EventRepository extends JpaRepository<Event, Long> {


    @Modifying
    @Transactional
    @Query(value = "DELETE FROM groups_events WHERE events_id = :events_id and groups_id = :groups_id", nativeQuery = true)
    public void deleteAssociatedEvent(@Param("groups_id") Long groupsId, @Param("events_id") Long eventId);

    @Modifying
    @Transactional
    @Query(value = "SELECT groups_id FROM groups_events WHERE events_id = :events_id", nativeQuery = true)
    public Long[] findGroups(@Param("events_id") Long eventId);


}
