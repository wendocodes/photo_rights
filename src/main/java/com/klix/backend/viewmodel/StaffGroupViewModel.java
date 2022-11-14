package com.klix.backend.viewmodel;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
    public class StaffGroupViewModel
    {
        private Long id;
        private Long groupId;
        private String firstName;
        private String lastName;
        private String email;
        private String groupName;
    }
