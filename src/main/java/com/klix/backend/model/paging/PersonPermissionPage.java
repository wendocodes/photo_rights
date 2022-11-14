package com.klix.backend.model.paging;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 */
@Getter
@Setter
public class PersonPermissionPage
{    
    private int pageNumber = 0;
    private int pageSize = 100000; // TODO: really realize paging
    private Sort.Direction sortDirection = Sort.Direction.ASC;
    private String[] sortBy = {"last_name", "first_name", "client_name", "client_role_name"};
}
