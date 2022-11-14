package com.klix.backend.service;

import java.util.List;

import com.klix.backend.model.Groups;
import com.klix.backend.repository.GroupsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


/**
 * Groups Service
 */
@Service
@Slf4j 
public class GroupsService
{
    @Autowired private GroupsRepository groupsRepository;


    /**
     * Save Groups
     */
    public void save(Groups group)
    {
        if (group == null) {
            log.info("No Groups found");
        } else {
            groupsRepository.save(group);
        }
        
        
    }

    /**
     * Find Group by id
     */
    public Groups findById(long id)
    {
        return groupsRepository.findById(id).orElse(null);
    }

    /**
     * edit Groups
     */
    public void edit(Long id, String name)
    {
        Groups existing = groupsRepository.findById(id).orElse(null);

        // save group
        if (existing == null) {
            this.save(new Groups());
        } else {
            existing.setGroupName(name);
            this.save(existing);
        }
    }

    
    /**
     * Find all groups
     */
    public List<Groups> findAll()
    {
        return groupsRepository.findAll();
    }



    
    /**
     * delete Person supervisor with the group
     */
    public void deleteSupervisorById(Long groups_id, Long person_id) {

        if(groups_id != null && person_id != null) {
            groupsRepository.deleteSupervisorById(groups_id, person_id);
        } else {
            log.info("PersonId or groupId is null at deleteAssociatedEvent in eventService.");
        }

    }
  
}