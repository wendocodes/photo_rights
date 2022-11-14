package com.klix.backend.model.interfaces;

import java.io.IOException;


/**
 * 
 */
public interface PictureInterface {

    public String getName();

    public String getType();

    public String getPictureString();

    public byte[] getPictureBytes();

    public String showClassification() throws IOException;


    public void setName(String name);

    public void setType(String type);

    public void setPictureBytes(byte[] bytes);
}