package com.klix.backend.service.picture;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.klix.backend.enums.AiContainer;
import com.klix.backend.enums.FaceFrameGenerationStatus;
import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;
import com.klix.backend.exceptions.FaceException;
import com.klix.backend.exceptions.GalleryPictureNotFoundException;
import com.klix.backend.exceptions.StorageException;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.api.cascadedetector.Request;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;
import com.klix.backend.repository.GalleryPictureRepository;
import com.klix.backend.service.DockerContainerService;
import com.klix.backend.service.JsonRequestService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FaceFrameService {

    @Autowired private JsonRequestService jsonRequestService;
    @Autowired private DockerContainerService dockerContainerService;

    @Autowired private PictureCropService pictureCropService;
    @Autowired private PictureFunctionService pictureFunctionService;
    
    @Autowired private GalleryPictureRepository galleryPictureRepository;
    @Autowired private GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;
    
    public GalleryPictureFaceFrame createFaceFrameGeneratedByHuman(long galleryPictureId, Integer[] coordinates) throws GalleryPictureNotFoundException, IOException
    {
        if (coordinates.length != 4) {
            throw new RuntimeException("coordinates does not have a length of 4.");
        }

        GalleryPicture img = galleryPictureRepository.findById(galleryPictureId).orElse(null);
        if (img == null) {
            throw new GalleryPictureNotFoundException();
        }
        
        GalleryPictureFaceFrame galleryPictureFaceFrame = new GalleryPictureFaceFrame();
        galleryPictureFaceFrame.setCoordinates(adjustCoordsToFitImage(img.getPictureBytes(), coordinates));
        galleryPictureFaceFrame.setIdentificationStatus(GalleryPictureFaceFrameIdentificationStatus.KI_WORK_NOT_DONE_NOT_REVIEWED);
        galleryPictureFaceFrame.setGallery_picture(img);
        galleryPictureFaceFrame.setGeneration_status(FaceFrameGenerationStatus.humanFrame);
        galleryPictureFaceFrame.setBlurryFaceFramePixels(computeBlurryFaceFrame(coordinates, PictureFunctionService.decompressBytes(img.getPictureBytes())));
        return galleryPictureFaceFrameRepository.save(galleryPictureFaceFrame);
    }

    private Integer[] adjustCoordsToFitImage(byte[] image, Integer[] coordinates) throws IOException
    {
        Integer[] faceCoordinates = new Integer[4];

        byte[] decodedString = PictureFunctionService.decompressBytes(image);
        ByteArrayInputStream bais = new ByteArrayInputStream(decodedString);
        BufferedImage img = ImageIO.read(bais);

        // x and w is mirrored
        if (coordinates[2] < 0) {
            coordinates[0] += coordinates[2];
            coordinates[2] *= -1;
        }
        // y and h is mirrored
        if (coordinates[3] < 0) {
            coordinates[1] += coordinates[3];
            coordinates[3] *= -1;
        }

        // all coordinates have a positive value since here.
        // correction terms such that frame closes with edges
        faceCoordinates[0] = clamp(0, coordinates[0], img.getWidth());   // x
        faceCoordinates[1] = clamp(0, coordinates[1], img.getHeight());  // y
        faceCoordinates[2] = clamp(0, coordinates[2], img.getWidth()-faceCoordinates[0]);  // w
        faceCoordinates[3] = clamp(0, coordinates[3], img.getHeight()-faceCoordinates[1]); // h
        return faceCoordinates;
    }
    private int clamp(int min, int value, int max) { return Math.max(min, Math.min(max, value)); }


    /**
	 * This method takes as an input the cropped gallerImage with only the face and sends it to the Identifikation-Container 
     * via a JsonRequest.
	 * @param String imageBitmap
	 * @return String neuralHash
	 * @throws StorageException
	 * @throws InterruptedException
	 */
    public String computeNeuralHash(byte[] face) throws StorageException, IOException 
    {
        String imageBitmapAsBase64 = Base64.getEncoder().encodeToString(face);

        String wcResponse = null;
        Request request = new Request("1", imageBitmapAsBase64);

        if(!dockerContainerService.isContainerUp(AiContainer.CNN))
        {
            throw new FaceException("ConnectionException");
        }


        try
        {
            wcResponse = jsonRequestService.sendJsonRequest(AiContainer.FACENET.getUrl() + "create_embeddingVector", request, String.class);
        } catch (WebClientResponseException e) {
            // get error cause from response
            log.warn(e.getResponseBodyAsString());
            return null;
        } catch (ConnectException e) {
            log.warn("Not connected");
            throw new FaceException("noface");
        }

        // process response & get face coordinates
        JsonObject responseBody = JsonParser.parseString(wcResponse).getAsJsonObject();

        if (!responseBody.get("neuralHash").isJsonArray())
        {
            throw new StorageException("neuralHash is not an array");
        }
              
        return responseBody.get("neuralHash").getAsJsonArray().toString();
    }


    public byte[] computeBlurryFaceFrame(Integer[] coordinate, byte[] img) {
        byte[] face = pictureCropService.cropImage(img, coordinate);
        byte[] blurredFace = {};

        try 
        {            
            blurredFace = arFeierabendBlur(face);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return blurredFace;
    }
    /**
     * for FaceFrames in GalleryPictures
     */
    public byte[] blur(byte[] image) throws IOException
    {
        // load image to blur
        ByteArrayInputStream bais = new ByteArrayInputStream(image);
        BufferedImage img = ImageIO.read(bais);
        
        // build kernel for the convolution
        int maxDim = Math.max(img.getWidth(), img.getHeight());
        Kernel kernel = constructKernel(maxDim);

        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        // convolute
        BufferedImage temp = op.filter(op.filter(img, null), null);

        //return image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(temp, "jpg", baos);
        baos.flush();
        byte[] blurry_imageInByte = baos.toByteArray();
        baos.close();
        return blurry_imageInByte;
    }
    
	Image arFeierabendBlur (BufferedImage originalImage, int areaPixelCount, double minDivisor) throws IOException {
	    
		double originalWidth=originalImage.getWidth();
		double originalHeight=originalImage.getHeight();
		double factor=Math.sqrt(Math.min(areaPixelCount/(originalWidth*originalHeight), 1/minDivisor));
		int targetWidth=(int)Math.round(originalWidth*factor);
		int targetHeight=(int)Math.round(originalHeight*factor);
		Image tmpImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_FAST);
	    return tmpImage.getScaledInstance(originalImage.getWidth(), originalImage.getHeight(), Image.SCALE_FAST);
	}
	
	public byte[] arFeierabendBlur (byte[] bytes) throws IOException {
		
		return arFeierabendBlur(bytes, 80, 10); 
	}
	
	public byte[] arFeierabendBlur (byte[] bytes, int areaPixelCount, double minDivisor) throws IOException {
		
		Image blurredImage=arFeierabendBlur(pictureFunctionService.bufferedImageFromByteArray(bytes),
				areaPixelCount, minDivisor);
		return pictureFunctionService.byteArrayFromImage(blurredImage, "JPG");
	}
	
    private Kernel constructKernel(int maxDimFace) {
        //divide by 16 to adjust kernel radius to image resolution
        int radius = maxDimFace/16 + 1;
        int size = radius * 2 + 1;
        
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];
        
        for (int i = 0; i < data.length; i++) {
            data[i] = weight;
        }

        return new Kernel(size, size, data);
    }
}
