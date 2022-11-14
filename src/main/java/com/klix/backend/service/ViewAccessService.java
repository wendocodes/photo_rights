package com.klix.backend.service;

import java.util.stream.Stream;
import java.util.Arrays;
import java.util.List;

import com.klix.backend.enums.ViewAccess;
import org.springframework.stereotype.Service;

import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.service.interfaces.SecurityService;

import com.klix.backend.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.klix.backend.model.Client;
import com.klix.backend.model.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ViewAccessService {
    @Autowired
    private ClientService clientService;
    @Autowired
    protected SecurityService securityService;

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * Hier stehen die Rechte der Reihe nach.
     */
    private ViewAccessDefinition[] viewAccessDefinitions = {
            // ViewName User MAdmin MA Kindbenutzer
            new ViewAccessDefinition("client/mitarbeiter/gallery", false, true, true, false),
            new ViewAccessDefinition("client/dashboard", false, true, true, false),
            new ViewAccessDefinition("client/mitarbeiter/groups", false, true, true, false),
            new ViewAccessDefinition("client/clientadmin/staff_table", false, true, true, false),
            new ViewAccessDefinition("client/mitarbeiter/groups_table", false, true, true, false),
            new ViewAccessDefinition("client/mitarbeiter/event_table", false, true, true, false),
            new ViewAccessDefinition("client/mitarbeiter/detection_table", false, true, true, false),
            new ViewAccessDefinition("client/mitarbeiter/identification_table", false, true, true, false),
            new ViewAccessDefinition("client/mitarbeiter/identificationPanel", false, true, true, false),
            new ViewAccessDefinition("client/mitarbeiter/personSearchPanel", false, true, true, false)
    };

    private class ViewAccessDefinition {
        private String viewName;
        private boolean accessUser;
        private boolean accessMitarbeiter;
        private boolean accessMAdmin;
        private boolean accessChildUser;

        public ViewAccessDefinition(String viewName,
                boolean accessUser,
                boolean accessMitarbeiter,
                boolean accessMAdmin,
                boolean accessChildUser) {
            this.viewName = viewName;
            this.accessUser = accessUser;
            this.accessMitarbeiter = accessMitarbeiter;
            this.accessMAdmin = accessMAdmin;
            this.accessChildUser = accessChildUser;
        }

        public boolean getAccess(int mandantRoleId) {

            switch (mandantRoleId) {
                case 1:
                    return accessMAdmin;
                case 2:
                    return accessMitarbeiter;
                case 3:
                    return accessUser;
                case 4:
                    return accessChildUser;
                default:
                    return false;
            }
        }
    }

    /**
     * Use this function to generically assign access rights to client pages
     */
    public ViewAccess checkPermissions(long clientId, String viewName) {
        // find Client by id
        Client client = clientService.findById(clientId);

        if (client.getId() == null) {
            return ViewAccess.FORBIDDEN;
        }

        User user = securityService.findLoggedInUser();

        long personId = user.getPerson().getId();
        List<PersonPermission> permissions = permissionRepository.findPermissionsByPersonAndClient(personId, clientId);

        // If no permissions are assigned to the current user, don't allow them to view
        // the page
        if (permissions == null || permissions.isEmpty()) {
            return ViewAccess.FORBIDDEN;
        }

        // For each permissions role id, allow acces as per the viewAccessDefinitions
        for (PersonPermission permission : permissions) {
            Stream<ViewAccessDefinition> stream = Arrays.stream(viewAccessDefinitions);
            ViewAccessDefinition accessView = stream.filter(o -> viewName.equals(o.viewName)).findFirst().get();
            boolean allowed = accessView.getAccess(Math.toIntExact(permission.getClient_role_id()));

            if (allowed) {
                return ViewAccess.ALLOWED;
            }
        }
        return ViewAccess.FORBIDDEN;
    }
}
