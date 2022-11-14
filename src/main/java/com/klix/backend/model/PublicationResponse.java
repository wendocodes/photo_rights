package com.klix.backend.model;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.klix.backend.enums.PublicationResponseStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PublicationResponse
{
    public PublicationResponse()
    {
        this.lastChanged = new Date();
        this.status = PublicationResponseStatus.PENDING;
    }
    public PublicationResponse(Long publicationRequestId, Long personId)
    {
        this();
        this.publicationRequestId = publicationRequestId;
        this.personId = personId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // need to set this on every save
    private Date lastChanged;

    private Long personId;

    private PublicationResponseStatus status;

    private Long publicationRequestId;
    
    /**
     * @author Andreas
     * @since 05/06 2022
     */
    @ElementCollection(fetch = FetchType.EAGER)//Eager notwendig zur Statusberechnung (in Klix. Außerhalb benötige ich Eager nicht)
    @CollectionTable(name = "MAP_RESP_PERSON_TO_STATUS")
    private Map<Long, PublicationResponseStatus> mapResponsiblepersonidToResponsestatus;//Kein camelCase wegen Hibernate 
    
    /**
     * @author Andreas
     * @since 05/06 2022
     */
    public void addResponsiblePersonIds (Set<Long> ids) {
    	
    	for (Long id : ids) addResponsiblePersonId(id);
    }
    
    /**
     * @author Andreas
     * @since 05/06 2022
     */
    public void addResponsiblePersonId (Long id) {
    	
    	if (mapResponsiblepersonidToResponsestatus==null) mapResponsiblepersonidToResponsestatus=new TreeMap<Long, PublicationResponseStatus>();
    	mapResponsiblepersonidToResponsestatus.put(id, PublicationResponseStatus.PENDING);
    }
 
    /**
     * @author Andreas
     * @since 05/06 2022
     */
    public void removeResponsiblePersonIds (Set<Long> ids) {
    	
    	if (mapResponsiblepersonidToResponsestatus!=null) {
    		for (Long id : ids) removeResponsiblePersonId(id);
    	}
    }
    
    /**
     * @author Andreas
     * @since 05/06 2022
     */
    public void removeResponsiblePersonId (Long id) {
    	
    	if (mapResponsiblepersonidToResponsestatus!=null) {
    		mapResponsiblepersonidToResponsestatus.remove(id);
    	}
    }
    
    /**
     * @author Andreas
     * @since 05/06 2022
     */
    public PublicationResponseStatus putStatus (Long responsiblePersonID, PublicationResponseStatus prs) {
    	
    	//if (respPersonIDToResp==null) respPersonIDToResp=new TreeMap<Long, PublicationResponseStatus>();
    	if (mapResponsiblepersonidToResponsestatus==null || !mapResponsiblepersonidToResponsestatus.containsKey(responsiblePersonID)) {//Der Fehler dürfte nicht vorkommen 
    		throw new RuntimeException("Die Person ist nicht berechtigt, über die Bildfreigabe zu bestimmen."
    				+"\nmapResponsiblepersonidToResponsestatus="+mapResponsiblepersonidToResponsestatus+", responsiblePersonID="+responsiblePersonID);
    	}
    	return mapResponsiblepersonidToResponsestatus.put(responsiblePersonID, prs);
    }
 
    /**
     * @author Andreas
     * @since 05/06 2022
     */
    public PublicationResponseStatus getStatus () {
    	
    	if (mapResponsiblepersonidToResponsestatus==null || mapResponsiblepersonidToResponsestatus.isEmpty()) return status;//Abwärtskompatibilität
    	status=PublicationResponseStatus.ALLOWED;
    	for (PublicationResponseStatus prs : mapResponsiblepersonidToResponsestatus.values()) {
    		if (prs.equals(PublicationResponseStatus.FORBIDDEN)) {
    			status=PublicationResponseStatus.FORBIDDEN;
    			return status;//Ende, wenn einer verbietet
    		}
    		if (prs.equals(PublicationResponseStatus.PENDING)) {
    			status=PublicationResponseStatus.PENDING;
    		}
    	}
    	return status;
    }
    
    /**
     * @author Andreas
     * @since 15.07.2022
     */
    public PublicationResponseStatus getStatus (Long responsiblePersonID) {
    	
    	if (mapResponsiblepersonidToResponsestatus==null) return null;//Sollte nicht vorkommen
    	return mapResponsiblepersonidToResponsestatus.get(responsiblePersonID);
    }
    
    /**
     * @author Andreas
     * @since 05/06 2022
     */
	@Override
	public String toString() {
		return "PublicationResponse [id=" + id + ", lastChanged=" + lastChanged + ", personId=" + personId + ", status="
				+ status + ", publicationRequestId=" + publicationRequestId
				+ ", mapResponsiblepersonidToResponsestatus=" + mapResponsiblepersonidToResponsestatus + "]";
	}
}