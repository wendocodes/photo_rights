package com.klix.backend.repository.projections;


/**
 * 
 */
public interface PersonPermission {   
    Long getId();
    String getLast_name();
    String getFirst_name();
    String getClient_name();
    String getClient_role_name();
    Long getClient_id();
    Long getPerson_id();
    Long getClient_role_id();
    Long getClient_person_id();
}