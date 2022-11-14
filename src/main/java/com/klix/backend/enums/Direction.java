package com.klix.backend.enums;

import org.springframework.data.domain.Sort;


public enum Direction {

    // has to be lowercase, Datatable sends it over that way
    asc(Sort.Direction.ASC),
    desc(Sort.Direction.DESC);

    private final Sort.Direction dir;

    Direction(Sort.Direction dir)
    {
        this.dir = dir;
    }

    public Sort.Direction getDir()
    {
        return this.dir;
    }

    public static Direction getInstance(Sort.Direction dir)
    {
        switch (dir)
        {
            case ASC: return Direction.asc;
            case DESC: return Direction.desc;

            default: return Direction.asc;
        }
    }
}