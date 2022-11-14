package com.klix.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.klix.backend.model.Client;
import com.klix.backend.model.Groups;

public interface GroupsRepository extends JpaRepository<Groups, Long>
{
    public List<Groups> findByClient(Client client);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM groups_members WHERE groups_id = :groups_id and members_id  = :members_id", nativeQuery = true)
    public void deleteGroupsMembersById(@Param("groups_id") Long groupsId, @Param("members_id") Long membersId);

    @Modifying
    @Transactional
    @Query(value = "SELECT groups_id FROM groups_supervisors WHERE supervisors_id = :supervisor_id", nativeQuery = true)
    public Long[] findSupervisor(@Param("supervisor_id") Long supervisorId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM groups_supervisors WHERE supervisor_id = :supervisor_id and groups_id = :groups_id", nativeQuery = true)
    // public void deleteSupervisorById(@Param("groups_id") Long groupsId, @Param("supervisor_id") Long supervisorId);
    public void deleteSupervisorById(@Param("groups_id") Long groupsId, @Param("supervisor_id") Long supervisorId);
}