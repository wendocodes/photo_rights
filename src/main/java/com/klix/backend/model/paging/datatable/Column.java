package com.klix.backend.model.paging.datatable;

import lombok.*;

// structure dictated by Datatable, package-private and only used in PagingRequest
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
class Column {
    private String data;
    private String name;
    private boolean searchable;
    private boolean orderable;
    private Search search;

    @Override
    public String toString() {
        return "{" +
            " data='" + data + "'" +
            ", name='" + name + "'" +
            ", searchable='" + searchable + "'" +
            ", orderable='" + orderable + "'" +
            ", search='" + search + "'" +
            "}";
    }
}