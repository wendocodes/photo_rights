package com.klix.backend.viewmodel;

import java.io.IOException;

import org.apache.commons.imaging.ImageReadException;

import com.klix.backend.model.interfaces.PictureInterface;
import com.klix.backend.service.picture.PictureFunctionService;


/**
 * Das PictureBaseViewModel dient dazu das Ergebnis der Klassifikation zu visualisieren
 * und die entsprechenden Methoden vom Daten-Model zu trennen.
 * 
 * Grundlegendes ViewModel für verschiedene Ansichten
 */
public class PictureBaseViewModel
{
    private PictureInterface picture;

    /**
     * Die Ergebniskoordinaten der Klassifikation.
     */
    private String coordinates = "";


    /**
     * 
     */
    public PictureBaseViewModel(PictureInterface picture, String coordinates)
    {
        this.picture = picture;
        this.coordinates = coordinates;   
    }
  

    /**
     * 
     */
	public String getType(){
        return this.picture.getType();
    }


    /**
     * 
     */
    public String getPictureString() {
        return this.picture.getPictureString();
    }


    /**
     * 
     */
    public String showClassification()
    throws IOException, ImageReadException {
        return PictureFunctionService.drawRectangle(this.coordinates, this.picture.getPictureBytes());
    }
}
