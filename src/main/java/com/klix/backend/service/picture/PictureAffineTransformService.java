package com.klix.backend.service.picture;

import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
/*
 * affine transformation applied to original image in case of, e.g. rotation
 */
public class PictureAffineTransformService {
    

    public byte[] applyTransform(byte[] uploadedImage, int orientation) throws IOException 
    {

        ByteArrayInputStream bis = new ByteArrayInputStream(uploadedImage);
        BufferedImage image = ImageIO.read(bis);
        
        //check if meta is not empty
        if(orientation != 0 && orientation != 1) {
            BufferedImage transformedImage = null;

            try {
                long mill1=System.currentTimeMillis();
                transformedImage = affineTransform(image, orientation);
                long mill2=System.currentTimeMillis();
                log.info("time for affine transform"+Long.toString(mill2-mill1));

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // convert back to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(transformedImage, "jpg", baos);
            baos.flush();
            log.info("size inside resize after procedure"+Integer.toString(baos.toByteArray().length));

            return baos.toByteArray();
        } else {
            log.info("Metadata could not be extracted or could not be used at applyTransform function.");
            return uploadedImage;
            
        }
    }
    
    public int readImageMeta(final byte[] imgFile) throws ImageReadException, IOException 
    {
        /** get all metadata stored in EXIF format (ie. from JPEG or TIFF). PNG does not contain EXIF data **/
        final ImageMetadata metadata = Imaging.getMetadata(imgFile);

        int orientation = 0;
        
        if (metadata instanceof JpegImageMetadata &&  (((JpegImageMetadata) metadata).findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION) != null)) {
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            orientation = jpegMetadata.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION).getIntValue();
            return orientation;
        }
        //tiff format is until now not even allowed but included here since we will probably use it later
        if (metadata instanceof TiffImageMetadata &&  (((TiffImageMetadata) metadata).findField(TiffTagConstants.TIFF_TAG_ORIENTATION) != null)) {
            TiffImageMetadata tiffMetadata = (TiffImageMetadata) metadata;
            orientation = tiffMetadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION).getIntValue();
            return orientation;
        }

        log.info("Could not read orientation at readImageMeta. Orientation is 0.");
        return orientation;
        
    }


    public BufferedImage affineTransform(BufferedImage image, int orientation) 
    {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean flipImageDimensions = false;
        
        AffineTransform t = new AffineTransform();
     
        //in every case, define affine transform and set buffered image according to orientation
        switch (orientation) {
            case 2: // Flip X
                t.scale(-1.0, 1.0);
                t.translate(-width, 0);
                break;
            case 3: // PI rotation 
                t.translate(width, height);
                t.rotate(Math.PI);
                break;
            case 4: // Flip Y
                t.scale(1.0, -1.0);
                t.translate(0, -height);
                break;
            case 5: // - PI/2 and Flip X
                t.rotate(-Math.PI / 2);
                t.scale(-1.0, 1.0);
                //interchange height and width for rotated image
                flipImageDimensions = true;
                break;
            case 6: // -PI/2 and -width
                t.translate(height, 0);
                t.rotate(Math.PI / 2);
                //interchange height and width for rotated image
                flipImageDimensions = true;
                break;
            case 7: // PI/2 and Flip
                t.scale(-1.0, 1.0);
                t.translate(-height, 0);
                t.translate(0, width);
                t.rotate(  3 * Math.PI / 2);
                //interchange height and width for rotated image
                flipImageDimensions = true;
                break;
            case 8: // PI / 2
                t.translate(0, width);
                t.rotate(  3 * Math.PI / 2);
                //interchange height and width for rotated image
                flipImageDimensions = true;
                break;

            default: return image;
        }

        AffineTransformOp op = new AffineTransformOp(t, AffineTransformOp.TYPE_BILINEAR);
        if (flipImageDimensions) {
            width = image.getHeight();
            height = image.getWidth();
        }
        BufferedImage destinationImage = new BufferedImage(width, height, image.getType());
        return op.filter(image, destinationImage);
     }
}