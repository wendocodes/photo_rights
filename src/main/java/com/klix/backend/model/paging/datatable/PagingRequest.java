package com.klix.backend.model.paging.datatable;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;

import lombok.*;


// structure dictated by Datatable
@Getter @Setter
public class PagingRequest {

    private long draw = 0;
    private int start = 0;    // page number
    private int length = 10;  // page size

    private Search search = new Search();
    private List<Order> order = new ArrayList<>();
    private List<Column> columns = new ArrayList<>();


    public int getPageIndex()
    {
        return this.start / this.length;
    }

    public Sort getSort()
    {
        List<Sort.Order> res = new ArrayList<>();
        for (Order o : this.order)
        {
            Column c = this.columns.get( o.getColumn() );
            if (c.isOrderable())
            {
                res.add(new Sort.Order(o.getSortDir(), c.getData()));
            }
        }
        return Sort.by(res);
    }

    @Override
    public String toString() {
        return "{" +
            " draw='" + draw + "'" +
            ", start='" + start + "'" +
            ", length='" + length + "'" +
            ", search='" + search + "'" +
            ", order='" + order + "'" +
            ", columns='" + columns + "'" +
            "}";
    }
}
