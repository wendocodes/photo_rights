package com.klix.backend.service.picture;

import com.klix.backend.enums.FaceFrameGenerationStatus;
import com.klix.backend.enums.GalleryImage_DetectionStatus;
import com.klix.backend.enums.GalleryPictureFaceFrameIdentificationStatus;
import com.klix.backend.exceptions.FaceException;
import com.klix.backend.exceptions.StorageException;
import com.klix.backend.model.GalleryPicture;
import com.klix.backend.model.GalleryPictureFaceFrame;
import com.klix.backend.model.GalleryPictureFaceFrameCosineDistances;
import com.klix.backend.model.IdPicture;
import com.klix.backend.model.Person;
import com.klix.backend.repository.GalleryPictureRepository;
import com.klix.backend.repository.IdPictureRepository;
import com.klix.backend.repository.PermissionRepository;
import com.klix.backend.repository.projections.PersonPermission;
import com.klix.backend.repository.GalleryPictureFaceFrameRepository;
import com.klix.backend.repository.GallyPictureFaceFrameCosineDistanceRepository;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PictureUploadService {

    @Autowired private IdPictureRepository idPictureRepository;

    @Autowired private GalleryPictureRepository galleryPictureRepository;
    @Autowired private GalleryPictureFaceFrameRepository galleryPictureFaceFrameRepository;
    @Autowired private GallyPictureFaceFrameCosineDistanceRepository galleryPictureFaceFrameCosineDistancesRepository;

    @Autowired private PermissionRepository permissionRepository;

    @Autowired private PictureCropService pictureCropService;
    @Autowired private PictureFunctionService pictureFunctionService;
    @Autowired private PictureAffineTransformService pictureAffineTransformService;
    @Autowired private FaceFrameService faceFrameService;

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    
    private static boolean threadsON=true;
    
    private class PipelineKiAndBlurPart implements Runnable {
    	
    	long imageId;
    	GalleryPicture galleryPicture=null;
    	byte[] transformedImage=null;
    	
    	PipelineKiAndBlurPart (long imageId) {
    		
    		this.imageId=imageId;
    	}
    	
    	PipelineKiAndBlurPart (GalleryPicture galleryPicture, byte[] transformedImage) {
    		
    		this.galleryPicture=galleryPicture;
    		this.transformedImage=transformedImage;
    	}
    	
    	public void run () {
    		
    		System.err.println("GalleryPicture Thread Start");
            if (galleryPicture==null) {
            	galleryPicture=galleryPictureRepository.findById(imageId).orElse(null);
                if (galleryPicture == null) {
                    log.info("GalleryPicture with id " + imageId + " could not be found.");
                    System.err.println("GalleryPicture Thread Ende mit Fehler.");
                    return;
                }

            	transformedImage=PictureFunctionService.decompressBytes(galleryPicture.getPictureBytes());
            }
            log.warn("IMAGE ID: " + galleryPicture.getId().toString());
            
            //compute the neuralHash and the coordinates for the faceFrame
            Integer[][] coordinates=null;
            try 
            {
                coordinates = detectFaces(transformedImage);
            } catch (FaceException | StorageException | IOException e) {
                // if no face was found on the image, return the image and process it further in detectionpanel
                // Ist sowieso schon returned (Andreas 03.09.2022)
            	e.printStackTrace();
            }
            
            //if no face was detected, we are done
            if (coordinates != null) 
            {
                try {
					saveFaceFrames(galleryPicture, transformedImage, coordinates);
				} catch (IOException | InterruptedException e) {
					//Für den Nutzer egal
					e.printStackTrace();
				}
            }
            System.err.println("GalleryPicture Thread Ende.");
    	}
    }

    /**
     * Image was upload from develop controller. Set person = null of IdPicture for testing in dev page
     */
    public IdPicture storeDevPicture(MultipartFile file) throws StorageException, IOException
    {
        // set person = null for testing in dev page
        return this.createIdPicture(file, null);
    }


    /**
     * Speichern eines Referenzbildes
     */
    @Transactional
    public IdPicture storeIdPicture(MultipartFile file, Person person) throws StorageException, IOException
    {
        // delete potential old one
        idPictureRepository.deleteByPersonId(person.getId());

        // save new profile image
        return this.createIdPicture(file, person);
    }

    private IdPicture createIdPicture(MultipartFile file, Person person) throws StorageException, IOException {
        
    	if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }
        
        //--------------------- Bild Teil: Skalierung und evtl Rotation ---------------------
        int orientation = 0;
        try {
            orientation = pictureAffineTransformService.readImageMeta(file.getBytes());
            log.info("orientation:"+Integer.toString(orientation));
        } catch (ImageReadException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        Image scaledImage=pictureFunctionService.scale(file.getBytes(), 1_280_000, false);
		String imageType=file.getContentType().split("/")[1];
        BufferedImage transFormedScaledImage=pictureAffineTransformService
        		.affineTransform(pictureFunctionService
        		.bufferedImageFromImage(scaledImage, imageType) , orientation); 
        byte[] transFormedScaledImageBytes=pictureFunctionService
        		.byteArrayFromImage(transFormedScaledImage, imageType);
        //--------------------- KI Teil: Detektion und Neural Hash ---------------------
        Integer[][] coordinates = pictureCropService.computeFaceCoordinates(transFormedScaledImageBytes);
        //if there was no face detected
        if (coordinates.length == 0) {
            throw new FaceException("NoFaceException");
        }
        //if there was more than one face detected
        if (coordinates.length > 1) {
            throw new FaceException("2face");
        }
        byte[] faceBytes = pictureFunctionService.byteArrayFromImage(transFormedScaledImage
        		.getSubimage(coordinates[0][0], coordinates[0][1], coordinates[0][2], coordinates[0][3])
        		,imageType);
        final String neuralHash = faceFrameService.computeNeuralHash(faceBytes);
        //--------------------- Model Teil: generate idPicture ---------------------
        IdPicture img = new IdPicture(
            file.getOriginalFilename(),
            file.getContentType(),
            PictureFunctionService.compressBytes(transFormedScaledImageBytes),
            //resizedImage,
            person
        );
        img.setNeuralHash(neuralHash);
        img.setKiPictureBytes(PictureFunctionService.compressBytes(faceBytes));    // save another version of the image bytes for face recognition
        //--------------------- Datenbank Teil ---------------------
        return idPictureRepository.save(img);
    }


    /**
     * Speichern eines Galleriebildes
     */
    public GalleryPicture storeGalleryPicture(MultipartFile file, Person person) throws StorageException, IOException, InterruptedException
    {
        
        if (file.isEmpty())
        {
            throw new StorageException("Failed to store empty file");
        }
        byte[] resizedImage = resizeImage(file.getBytes());//Andreas 03.09.22: Wir versuchen jetzt bis 10 MB ohne resize 
        byte[] transformedImage = rotate(file, resizedImage);
        GalleryPicture img = saveGalleryPicture(file, transformedImage, person);
        if (threadsON) {
			if (executor.getActiveCount() < executor.getCorePoolSize()) {
                // no thread is running right now
                // (maximum amount of threads is 1, anything smaller has to be 0 threads running, see construction of executor-object)
				executor.submit(new PipelineKiAndBlurPart(img, transformedImage));
			}
			else {
                // thread pool is full, newly created ones will be queued
                // (in this case 1 thread is the maximum, see construction of executor-object)
                // thread should NOT keep the whole image in memory while waiting for execution.
                // the image will be fetched on run from the db
				executor.submit(new PipelineKiAndBlurPart(img.getId()));
			}
		}
		else {
			new PipelineKiAndBlurPart(img, transformedImage).run();
		}
        return img;
    }

    private byte[] rotate(MultipartFile file, byte[] data) throws IOException {
        //get orientation encoded as integer to see what that means consider https://sirv.com/help/articles/rotate-photos-to-be-upright/
        int orientation = 0;
        try {
            orientation = pictureAffineTransformService.readImageMeta(file.getBytes());
            log.info("orientation:"+Integer.toString(orientation));
        } catch (ImageReadException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        //affine transformation applied to original image in case of, e.g. rotation
        return pictureAffineTransformService.applyTransform(data, orientation);
    }

    private GalleryPicture saveGalleryPicture(MultipartFile file, byte[] image, Person person) {
        //generate galleryImage
        GalleryPicture img = new GalleryPicture();

        //only employees are allowed to upload galleryPicture for now, thus the clientId is unique and must exist
        List<PersonPermission> permissions = permissionRepository.findPermissionByPersonId(person.getId());
        Long clientId = permissions.get(0).getClient_id();
        
        img.setName(file.getOriginalFilename());
        img.setType(file.getContentType());
        img.setPictureBytes(PictureFunctionService.compressBytes(image));
        img.setUploader(person);
        img.setStatus(GalleryImage_DetectionStatus.neverReviewed);
        img.setClientId(clientId);
        img.setLastEdited(LocalDate.now());//Andreas: Finde ich eigentlich nicht richtig. Wurde ja nicht editiert.
        img.setUploadDate(LocalDate.now());
        
        return galleryPictureRepository.save(img);
    }

    private Integer[][] detectFaces(byte[] image) throws StorageException, IOException {
        return pictureCropService.computeFaceCoordinates(image);
    }

    private void saveFaceFrames(GalleryPicture img, byte[] image, Integer[][] coordinates) throws IOException, InterruptedException
    {
        //iterate over all faces of a given galleryPicture
        //and generate galleryPictureFaceFrameCosineDistances for each face frame
        for (Integer[] faceCoordinates : coordinates) 
        {
            byte[] face = pictureCropService.cropImage(image, faceCoordinates);
            String neuralHash = faceFrameService.computeNeuralHash(face);

            //for each neuralHash generate one faceFrame
            GalleryPictureFaceFrame galleryPictureFaceFrame = new GalleryPictureFaceFrame(
                faceCoordinates,
                neuralHash,
                img,
                /* Andreas 03.09.2022
                 * In computeBlurryFaceFrame wird das Gesicht noch einmal mit pictureCropService.cropImage ausgeschnitten.
                 * Das ist nicht nötig, wir können direkt blur aufrufen mit den oben ausgeschnittetenem face
                 * faceFrameService.computeBlurryFaceFrame(faceCoordinates, PictureFunctionService.decompressBytes(img.getPictureBytes())),
                 */
                //faceFrameService.blur(face),      // the 'old' blur
                faceFrameService.arFeierabendBlur(face),
                FaceFrameGenerationStatus.kiFrame,
                GalleryPictureFaceFrameIdentificationStatus.KI_WORK_NOT_DONE_NOT_REVIEWED
            );
            //Andreas: Ich setze den IdentificationStatus zwei mal, weil vernünftigerweise später mal die Aufgaben parallelisiert werden sollten und das dann nicht vergessen wird.
            //generate Treemap which is ordered set with pair of smallest distance and associated personId
            //as the first entry 
            TreeMap<Double,Long> recognizePerson_treeMap = recognizePerson_Treemap(neuralHash, img.getClientId());
            computeAndSaveFaceFrameCosineDistance(recognizePerson_treeMap, galleryPictureFaceFrame);
            galleryPictureFaceFrame.setIdentificationStatus(GalleryPictureFaceFrameIdentificationStatus.KI_WORK_DONE_NOT_REVIEWED);
            galleryPictureFaceFrameRepository.save(galleryPictureFaceFrame);
        }
    }
         /**
      * TODO make scalable. This is at least O(n) with n = #Person at same client
	  * This method takes as an input the neuralHashed and computes the distance to all 
      * IdPicture of the same client and returns an ordered distanceIdPairMap of distance and Id
      * @param clientId
	  * @param String neuralHashes
	  * @return TreeMap<Double,Long> cosine distance as Double-key, personId as Long. Will be ordered asc by cos-dist, because it is a tree map.
	  */
    private TreeMap<Double, Long> recognizePerson_Treemap(String neuralHash, Long clientId)
    {
        List<IdPicture> idPictures = idPictureRepository.findByClientId(clientId);
        TreeMap<Double, Long> distanceIdPairMap = new TreeMap<>();
        log.info("check idpictures size {}", idPictures.size());
        for (IdPicture idPicture : idPictures) 
        {
            long personId = idPicture.getPerson().getId();

            double cosineDistance = cosineDistance(idPicture.getNeuralHash(), neuralHash);
            log.info("check key cosine distance:{} check value person_id: {}", cosineDistance, personId);
            distanceIdPairMap.put(cosineDistance, personId);
        }

        return distanceIdPairMap;
    }
    /* Helper Function */
    private double cosineDistance(String face1, String face2) throws InputMismatchException
    {
        final int ARRAY_LENGTH = 512;

        //init variables
        final JSONArray face1Array = new JSONArray(face1);
        final JSONArray face2Array = new JSONArray(face2);
        if (face1Array.length() != ARRAY_LENGTH || face2Array.length() != ARRAY_LENGTH)
        {
            throw new InputMismatchException("face-array of ai does not have " + ARRAY_LENGTH + " entries.");
        }
        
        double dotProduct = 0.0;
        double face1Metrik = 0.0;
        double face2Metrik = 0.0;
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            double face1Coord = face1Array.getDouble(i);
            double face2Coord = face2Array.getDouble(i);

            //compute the dot product
            dotProduct += face1Coord * face2Coord;
            
            //compute the l2 norm of the vectorRepresentations of the face
            face1Metrik += face1Coord * face1Coord;
            face2Metrik += face2Coord * face2Coord;
        }

        //1 - cosinus similarity such that similiar values have a low cosine distance
        return 1-(dotProduct / Math.sqrt(face1Metrik*face2Metrik));
    }

    private byte[] resizeImage(byte[] input)
        throws IOException {

        int MAX_SIZE = 12_000_000;//500000; Wir wollen 10 MB zulassen. Puffer wegen unterschiedlicher Berechnungen 

        // check if image is resized to MAX_SIZE
        if(input.length <= MAX_SIZE) 
        {
            return input;
        }

        // convert bytes to buffered image
        ByteArrayInputStream bis = new ByteArrayInputStream(input);
        BufferedImage originalImage = ImageIO.read(bis);

        // resize the image
        float scaleBy = (float) Math.sqrt((float) MAX_SIZE / input.length);
        int width = (int)Math.floor(originalImage.getWidth() * scaleBy);

        // if getScaledInstance is given either height or width as a negative number, it will resize by preserving the ratio between height / width
        Image newResizedImage = originalImage.getScaledInstance(width, -1, Image.SCALE_FAST);
        
        // convert back to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(convertToBufferedImage(newResizedImage), "jpg", baos);
        baos.flush();

        return baos.toByteArray();
    }
    private BufferedImage convertToBufferedImage(Image img)
    {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bi = new BufferedImage(
            img.getWidth(null),
            img.getHeight(null),
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics2D = bi.createGraphics();
        graphics2D.drawImage(img, 0, 0, null);
        graphics2D.dispose();
        return bi;
    }


    private void computeAndSaveFaceFrameCosineDistance(TreeMap<Double, Long> recognizePerson_treeMap, GalleryPictureFaceFrame faceFrame)
    {
        int personCounter = 0;
        Set<GalleryPictureFaceFrameCosineDistances> cosineDistances = new HashSet<>();

        for(Map.Entry<Double,Long> entry : recognizePerson_treeMap.entrySet()) 
        {
            Double cosineDistance = entry.getKey();
            Long recognized_personId = entry.getValue();

            //Andreas: Sonst wird unnötig die komplette Map durchlaufen
            // Wir brauchen aber nur die besten 5 Vorschläge
            if (personCounter++ >= 5) { break; }

            //take the best 5 FaceFrames with lowest cosineDistance since treeMap is already ordered
            GalleryPictureFaceFrameCosineDistances dist = new GalleryPictureFaceFrameCosineDistances(recognized_personId, cosineDistance, faceFrame);
            cosineDistances.add(dist);
        }
        galleryPictureFaceFrameCosineDistancesRepository.saveAll(cosineDistances);
    }
}
