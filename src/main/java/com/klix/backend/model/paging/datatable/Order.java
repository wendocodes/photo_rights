package com.klix.backend.model.paging.datatable;

import org.springframework.data.domain.Sort;
import com.klix.backend.enums.Direction;

import lombok.*;


// structure dictated by Datatable, package-private and only used in PagingRequest
@Setter @Getter @AllArgsConstructor @NoArgsConstructor
class Order {

    private Integer column;
    private Direction dir;


    public Sort.Direction getSortDir() {
        return this.dir.getDir();
    }

    public void setSortDir(Sort.Direction dir) {
        this.dir = Direction.getInstance(dir);
    }


    @Override
    public String toString() {
        return "{" +
            " column='" + column + "'" +
            ", dir='" + dir.name() + "'" +
            "}";
    }
}