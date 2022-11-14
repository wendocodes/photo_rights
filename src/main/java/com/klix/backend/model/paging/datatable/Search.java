package com.klix.backend.model.paging.datatable;

import lombok.*;

// structure dictated by Datatable, package-private and only used in PagingRequest
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
class Search {

    private String value;
    private boolean regexp;

    @Override
    public String toString() {
        return "{" +
            " value='" + value + "'" +
            ", regexp='" + regexp + "'" +
            "}";
    }
}
