package com.klix.backend.service.picture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import javax.imageio.ImageIO;

import com.klix.backend.controller.client.employees.IdentificationPanelController;
import com.klix.backend.enums.FaceFrameGenerationStatus;
import com.klix.backend.model.FrameIdentificationReview;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.repository.FrameIdentificationReviewRepository;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;

import java.awt.BasicStroke;

import org.apache.commons.imaging.ImageReadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


/**
 * 
 */
@Slf4j
@Service
public class PictureFunctionService 
{

    @Autowired private GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;
    @Autowired private FrameIdentificationReviewRepository frameIdentificationReviewRepository;
    @Autowired IdentificationPanelController identificationPanelController;


    /**
     * Compress the image bytes before storing it in the database
     */
    public static byte[] compressBytes(byte[] data) 
    {
        if (data == null) 
        {
            log.info("compressBytes input data is empty");
            return data;
        }

        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        try {
            outputStream.close();
        } 
        catch (IOException e) {}

        return outputStream.toByteArray();
    }


    /**
     * Uncompress the image bytes before returning it to the angular application
     */
    public static byte[] decompressBytes(byte[] data) 
    {
        if (data == null) 
        {
            log.info("decompressBytes input data is empty");
            return data;
        }

        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];

        try {
            while (!inflater.finished()) 
            {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
        } 
        catch (IOException | DataFormatException e) {}

        return outputStream.toByteArray();
    }

    /**
     * Draw from an array of integer coordinates on by reading first status of image
     */
    public static String drawManyRectangles(Map<Integer[], FaceFrameGenerationStatus> drawingMap, byte[] encoded_image) throws IOException
    {
        byte[] decodedString = PictureFunctionService.decompressBytes(encoded_image);
        ByteArrayInputStream bais = new ByteArrayInputStream(decodedString);

        BufferedImage img = ImageIO.read(bais);
        if (img == null) 
        {
            log.info("Image is null");
            return "";
        }

        Graphics2D graphics = (Graphics2D)img.getGraphics();

        for (Entry<Integer[], FaceFrameGenerationStatus> entry : drawingMap.entrySet())
        {
            FaceFrameGenerationStatus generationStatus = entry.getValue();
            if(generationStatus == FaceFrameGenerationStatus.kiFrame)     graphics.setColor(java.awt.Color.red);
            if(generationStatus == FaceFrameGenerationStatus.humanFrame)  graphics.setColor(java.awt.Color.green);

            graphics.setStroke(new BasicStroke(5));
            Integer[] coordinate = entry.getKey();
            if(coordinate.length == 4) {
                graphics.drawRect(coordinate[0], coordinate[1], coordinate[2], coordinate[3]);
                
            } else {
                log.info("Coordinates are empty.");
                return "";
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //Start Andreas 9.5.22 Bugfix 26685
        String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(decodedString));
        if (contentType!=null && contentType.equals("image/png")) {
        	BufferedImage newImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        	newImage.createGraphics().drawImage(img, 0, 0, Color.WHITE, null);
        	ImageIO.write(newImage, "jpg", baos);
        }
        else ImageIO.write(img, "jpg", baos);
        //Ende Andreas 9.5.22 Bugfix 26685
        baos.flush();
        byte[] imageInByte = baos.toByteArray();            
        baos.close();
        return Base64.getEncoder().encodeToString(imageInByte);
    }


    /**
    *   Draw from an array of integer coordinates the blurred Images into the whole image of all faces
        except the receiver
    */
    public byte[] drawBluredFacesExceptReceiver(Long personId, GalleryPicture galleryPicture) throws IOException
    {
    	TreeSet<Long> set=new TreeSet<>();
    	set.add(personId);
    	return drawBluredFacesExceptPersons(set, galleryPicture);
    }
    	
    public byte[] drawBluredFacesExceptPersons (Set<Long> personIds, GalleryPicture galleryPicture) throws IOException
    {
        //Set<GalleryPictureFaceFrame> faceFrames = galleryPicture.getGalleryPictureFaceFrame();
        List<GalleryPictureFaceFrame> faceFrames = galleryPictureFaceFrameRepository.findByGalleryPictureId(galleryPicture.getId());

        //decode galleryImage to blur
        byte[] decodedString = PictureFunctionService.decompressBytes(galleryPicture.getPictureBytes());
        ByteArrayInputStream bais = new ByteArrayInputStream(decodedString);
        BufferedImage img = ImageIO.read(bais);

        for (GalleryPictureFaceFrame galleryPictureFaceFrame : faceFrames) 
        {
            
            List<FrameIdentificationReview> faceFrameIds =  frameIdentificationReviewRepository.findAllByFrameID(galleryPictureFaceFrame.getFaceFrameId());
            FrameIdentificationReview faceFrameId = null;
            if(!faceFrameIds.isEmpty()) 
            {
                faceFrameId = identificationPanelController.lastOf(faceFrameIds, 1).get(0);
            }

            //blurr all faces except the one of the person that is viewing it with input personId
            Long personIdOnImage = null;
            if (faceFrameId != null) personIdOnImage = faceFrameId.getPersonID();
 
            if (faceFrameId == null || personIdOnImage==null || !personIds.contains(personIdOnImage))
            {
                Integer[] coordinate = galleryPictureFaceFrame.getCoordinates();
            
                byte[] blurryDecodedImage = galleryPictureFaceFrame.getBlurryFaceFramePixels();
                ByteArrayInputStream blurryBais = new ByteArrayInputStream(blurryDecodedImage);
                BufferedImage blurryFacePixel = ImageIO.read(blurryBais);
        
                // crop IdPicture to face coordinates, containing only detected face
                WritableRaster croppedImage = img.getSubimage(coordinate[0], coordinate[1], coordinate[2], coordinate[3]).getRaster();
                
                blurryFacePixel.copyData(croppedImage);
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        ImageIO.write( img, "jpg", baos);
        baos.flush();
        byte[] imageInByte = PictureFunctionService.compressBytes(baos.toByteArray());            
        baos.close();
        return imageInByte;
    }

    
    /**
     * 
     */
    public static String drawRectangle(String coordinates, byte[] encoded_image)
    throws ImageReadException, IOException
    {
        // Falls das Bild oder die Koordinaten leer sind, gebe leeren String zurück.
        if (coordinates == null || encoded_image == null)
        {
            log.debug("drawRectangle input data is empty");
            return " ";
        }
        else
        {
            // Die folgenden Zeilen waren notwendig, da Json String nicht richtig gemappt werden konnte.

            // Entferne alle Zeichen vom Koordinatenstring, die keine Zahlen sind:
            String[] coordinate_array_string = coordinates
                            .replaceAll("\"", "")
                            .replaceAll("\\[", "")
                            .replaceAll("\\]", "")
                            .replaceAll(" ", "")
                            .split(",");

            // Parse Zahlen zu Integern und lege sie in Koordinaten array ab.
            int[][] coordinate_array_int = new int[coordinate_array_string.length-1][4];
            int k = 0;
            for (int i = 0; i < coordinate_array_string.length / 4; i++)
            {
                for (int j = 0; j < 4; j++)
                {
                    coordinate_array_int[i][j] = Integer.parseInt(coordinate_array_string[k]);
                    k += 1;
                }
            }
        
            byte[] decodedString = PictureFunctionService.decompressBytes(encoded_image);
            ByteArrayInputStream bais = new ByteArrayInputStream(decodedString);
            BufferedImage img = null;
            
            try {
                img = ImageIO.read(bais);
            }
            catch (IOException e) {
                throw new ImageReadException(e.getMessage(), e);
            }

            for (int i = 0; i < coordinate_array_string.length/4; i++)
            {
                int[] face = coordinate_array_int[i];
                img.getGraphics().drawRect(face[0], face[1], face[2], face[3]);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write( img, "jpg", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();            
            baos.close();
   
            return Base64.getEncoder().encodeToString(imageInByte);
        }
    }
    
	/* Diese Hin und Her-Transformationen sollte man verbessern / vermeiden
	 */
	public byte[] byteArrayFromImage (Image image, String imageFileFormat) throws IOException {
		
	    BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
	    Graphics2D g2d = bi.createGraphics();
	    g2d.drawImage(image, 0, 0, null);
	    g2d.dispose();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(bi, imageFileFormat, buffer);
        return buffer.toByteArray();
	}
	
	/* Diese Hin und Her-Transformationen sollte man verbessern / vermeiden
	 */
	public BufferedImage bufferedImageFromByteArray (byte[] array) throws IOException {
		
		return ImageIO.read(new ByteArrayInputStream(array));
	}
	
	/* Diese Hin und Her-Transformationen sollte man verbessern / vermeiden
	 */
	public BufferedImage bufferedImageFromImage (Image image, String imageFileFormat) throws IOException {
		
		if (image instanceof BufferedImage) return (BufferedImage)image;
		return bufferedImageFromByteArray(byteArrayFromImage(image, imageFileFormat));
	}
	
	/**
	 * 
	 * @param imageAsArray
	 * @param targetPixel Gesamtanzahl der Pixel, die das Bild haben soll.
	 * @param allowEnlargement true: wenn auch vergrößert werden darf
	 * @return
	 * @throws IOException
	 */
    public Image scale (byte[] imageAsArray, long targetPixel, boolean allowEnlargement) throws IOException {
    	
    	return scale(bufferedImageFromByteArray(imageAsArray), targetPixel, allowEnlargement);
    }
    
    /**
     * @author Andreas
     * @param originalImage
     * @param targetPixel: Die Anzahl der Pixel, die die Flaeche(!) des Bildes am Ende haben soll.
     * @param allowEnlargement true: wenn auch vergrößert werden darf
     * @return
     * @throws IOException
     * @since 20.09.2022
     */
	public Image scale (BufferedImage originalImage, long targetPixel, boolean allowEnlargement) throws IOException {

		double originalWidth=originalImage.getWidth();
		double originalHeight=originalImage.getHeight();
		double oldPixel=originalWidth*originalHeight;
		if (oldPixel==targetPixel || (!allowEnlargement && oldPixel<targetPixel)) return originalImage;
		double factor=Math.sqrt(targetPixel/oldPixel);
		int targetWidth=(int)Math.round(originalWidth*factor);
		Image tmp=originalImage.getScaledInstance(targetWidth, -1, Image.SCALE_FAST);
		return tmp;
	}
}