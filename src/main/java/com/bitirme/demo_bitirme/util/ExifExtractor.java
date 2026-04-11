package com.bitirme.demo_bitirme.util;

import com.bitirme.demo_bitirme.data.dto.EXIFDataDTO;
import com.bitirme.demo_bitirme.exception.ExifExtractionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ExifExtractor {

    public EXIFDataDTO extractExifData(File imageFile) {
        try {
            ImageMetadata metadata = Imaging.getMetadata(imageFile);
            if (!(metadata instanceof JpegImageMetadata jpegMetadata)) {
                log.warn("File is not a JPEG with EXIF data: {}", imageFile.getName());
                return null;
            }

            EXIFDataDTO exifData = new EXIFDataDTO();
            TiffImageMetadata exifMetadata = jpegMetadata.getExif();

            if (exifMetadata != null) {
                // Extract camera make
                String[] cameraMake = exifMetadata.getFieldValue(TiffTagConstants.TIFF_TAG_MAKE);
                exifData.setCameraMake(cameraMake != null ? cameraMake[0] : null);

                // Extract camera model
                String[] cameraModel = exifMetadata.getFieldValue(TiffTagConstants.TIFF_TAG_MODEL);
                exifData.setCameraModel(cameraModel != null ? cameraModel[0] : null);

                // Extract ISO
                short[] isoValues = exifMetadata.getFieldValue(ExifTagConstants.EXIF_TAG_ISO);
                if (isoValues != null && isoValues.length > 0) {
                    exifData.setIso((int) isoValues[0]);
                }

                // Extract aperture (F-number)
                RationalNumber[] apertureValues = exifMetadata.getFieldValue(ExifTagConstants.EXIF_TAG_FNUMBER);
                if (apertureValues != null && apertureValues.length > 0) {
                    try {
                        exifData.setAperture(BigDecimal.valueOf(apertureValues[0].doubleValue()));
                    } catch (Exception e) {
                        log.debug("Could not parse aperture: {}", (Object) apertureValues);
                    }
                }

                // Extract exposure time
                RationalNumber[] exposureValues = exifMetadata.getFieldValue(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME);
                if (exposureValues != null && exposureValues.length > 0) {
                    exifData.setExposureTime(exposureValues[0].numerator + "/" + exposureValues[0].divisor);
                }

                // Extract focal length
                RationalNumber[] focalLengthValues = exifMetadata.getFieldValue(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH);
                if (focalLengthValues != null && focalLengthValues.length > 0) {
                    try {
                        exifData.setFocalLength(BigDecimal.valueOf(focalLengthValues[0].doubleValue()));
                    } catch (Exception e) {
                        log.debug("Could not parse focal length: {}", (Object) focalLengthValues);
                    }
                }

                // Extract date taken
                String[] dateTimeValues = exifMetadata.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                if (dateTimeValues != null && dateTimeValues.length > 0) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                        exifData.setDateTaken(LocalDateTime.parse(dateTimeValues[0], formatter));
                    } catch (Exception e) {
                        log.debug("Could not parse date taken: {}", (Object) dateTimeValues);
                    }
                }

                // Extract orientation
                Object orientationObject = exifMetadata.getFieldValue(TiffTagConstants.TIFF_TAG_ORIENTATION);
                if (orientationObject != null) {
                    try {
                        exifData.setOrientation(Integer.parseInt(orientationObject.toString()));
                    } catch (Exception e) {
                        log.debug("Could not parse orientation: {}", orientationObject);
                    }
                }
            }

            log.debug("Successfully extracted EXIF data from: {}", imageFile.getName());
            return exifData;

        } catch (ImageReadException | IOException e) {
            log.warn("Failed to extract EXIF data: {}", e.getMessage());
            throw new ExifExtractionException("Could not extract EXIF data from image", e);
        }
    }
}
