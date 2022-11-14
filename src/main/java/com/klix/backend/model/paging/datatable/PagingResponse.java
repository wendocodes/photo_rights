package com.klix.backend.model.paging.datatable;

import java.util.List;

// structure dictated by Datatable
public class PagingResponse<T> {

    public PagingResponse() { }
    public PagingResponse(long draw, long recordsTotal, long recordsFiltered, List<T> data) {
        this.draw = draw;
        this.recordsTotal = recordsTotal;
        this.recordsFiltered = recordsFiltered;
        this.data = data;
    }


    private long draw = 0;
    private long recordsTotal = 0;
    private long recordsFiltered = 0;

    private List<T> data = null;
    

    public long getDraw() {
        return this.draw;
    }

    public void setDraw(long draw) {
        this.draw = draw;
    }

    public long getRecordsTotal() {
        return this.recordsTotal;
    }

    public void setRecordsTotal(long recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public long getRecordsFiltered() {
        return this.recordsFiltered;
    }

    public void setRecordsFiltered(long recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public List<T> getData() {
        return this.data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}