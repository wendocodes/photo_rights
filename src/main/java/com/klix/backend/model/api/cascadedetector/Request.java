package com.klix.backend.model.api.cascadedetector;

/**
 * Hilfs-Model für Anfragen zur AI
 */
public class Request
{    
    private String id;
    private String bitmap;

    public Request(String img_id, String bitmap)
    {
        this.id = img_id;
        this.bitmap = bitmap;
    }
    
    public String getbitmap() {
        return bitmap;
    }

    public void setbitmap(String bitmap) {
        this.bitmap = bitmap;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Request {" + "id='" + id + '}';
    }
}