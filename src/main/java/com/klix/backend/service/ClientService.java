package com.klix.backend.service;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import com.klix.backend.model.Client;
import com.klix.backend.model.paging.datatable.PagingRequest;
import com.klix.backend.model.paging.datatable.PagingResponse;
import com.klix.backend.repository.ClientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


/**
 * 
 */
@Service
@Slf4j  // logger
public class ClientService
{
    @Autowired
    private ClientRepository clientRepository;


    /**
     * 
     */
    public Client save(Client client)
    {
        if (client == null)
        {
            log.info("Client not found on save. Exit.");
            return null;
        }
        return clientRepository.save(client);
    }

    
    /**
     * 
     */
    public Client findById(long id)
    {
        return clientRepository.findById(id).orElse(null);
    }


    /**
     * 
     */
    public Client findByName(String name)
    {
        return clientRepository.findByName(name).orElse(null);
    }


    /**
     * ! Das Löschen von Client mit zugeordneten Datensätzen muss noch implementiert werden. 
     * 
     * Löscht aber schon Client ohne zugeordnete Datensätze.
     * 
     * @param id Die Id des Client
     */
    public void deleteById(long id) {
        clientRepository.deleteById(id);
    }


    /**
     * 
     */
    public Client edit(Client client)
    {
        Client existing = clientRepository.findById( client.getId() ).orElse(null);

        // not jet saved, so just save the new one
        if (existing == null) return this.save(new Client( client.getName() ));

        existing.setName( client.getName() );

        return this.save(existing);
    }


    /**
     * 
     */
    public Client edit(long id, String name)
    {
        Client existing = clientRepository.findById(id).orElse(null);

        // not jet saved, so just save the new one
        if (existing == null) return this.save(new Client());

        existing.setName(name);

        return this.save(existing);
    }

    
    /**
     * 
     */
    public List<Client> findAll()
    {
        return clientRepository.findAll();
    }

    
    /**
     * 
     */
    public Client getOne(long id) throws EntityNotFoundException
    {
        return clientRepository.getOne(id);
    }

    
    /**
     * 
     */
    public void	deleteInBatch(Iterable<Client> clients)
    {
        clientRepository.deleteInBatch(clients);
    }


    /**
     * 
     */
    public PagingResponse<Client> getPage(PagingRequest request)
    {
        Pageable pageable = PageRequest.of(request.getPageIndex(), request.getLength(), request.getSort());
        Page<Client> page = clientRepository.findAll(pageable);

        // TODO: update second page.getTotalElements() to something for Sort
        return new PagingResponse<>(request.getDraw(), page.getTotalElements(), page.getTotalElements(), page.getContent());
    }
}