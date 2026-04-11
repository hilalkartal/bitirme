package com.bitirme.demo_bitirme.service;

import com.bitirme.demo_bitirme.config.UploadConfig;
import com.bitirme.demo_bitirme.data.dto.EXIFDataDTO;
import com.bitirme.demo_bitirme.data.dto.GPSDataDTO;
import com.bitirme.demo_bitirme.data.dto.PhotoDTO;
import com.bitirme.demo_bitirme.data.entity.ExifData;
import com.bitirme.demo_bitirme.data.entity.GpsData;
import com.bitirme.demo_bitirme.data.entity.Photo;
import com.bitirme.demo_bitirme.data.mapper.PhotoMapper;
import com.bitirme.demo_bitirme.exception.ExifExtractionException;
import com.bitirme.demo_bitirme.exception.FileStorageException;
import com.bitirme.demo_bitirme.exception.PhotoNotFoundException;
import com.bitirme.demo_bitirme.exception.PhotoUploadException;
import com.bitirme.demo_bitirme.repository.ExifDataRepository;
import com.bitirme.demo_bitirme.repository.GpsDataRepository;
import com.bitirme.demo_bitirme.repository.PhotoRepository;
import com.bitirme.demo_bitirme.util.ExifExtractor;
import com.bitirme.demo_bitirme.util.ReverseGeocoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final ExifDataRepository exifDataRepository;
    private final GpsDataRepository gpsDataRepository;
    private final PhotoMapper photoMapper;
    private final UploadConfig uploadConfig;
    private final ExifExtractor exifExtractor;
    private final ReverseGeocoder reverseGeocoder;

    /**
     * Upload a photo and extract EXIF/GPS metadata
     */
    public PhotoDTO uploadPhoto(MultipartFile file) {
        log.info("Starting photo upload: {}", file.getOriginalFilename());

        try {
            // Validate file
            validatePhotoFile(file);

            // Create upload directory if not exists
            uploadConfig.ensureUploadDirExists();

            // Generate unique filename
            String uniqueFileName = generateUniqueFilename(file.getOriginalFilename());
            Path uploadPath = uploadConfig.getUploadPath().resolve(uniqueFileName);

            // Save file to disk
            Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File saved to: {}", uploadPath);

            // Create and save Photo entity first so it gets an ID
            Photo photo = new Photo();
            photo.setFileName(uniqueFileName);
            photo.setFilePath(uploadPath.toString());
            Photo savedPhoto = photoRepository.save(photo);
            log.debug("Photo entity saved with ID: {}", savedPhoto.getId());

            // Extract EXIF data (photo must already be saved)
            ExifData exifData = extractAndSaveExifData(savedPhoto, uploadPath.toFile());
            if (exifData != null) {
                savedPhoto.setExifData(exifData);
            }

            // Extract GPS data from EXIF (photo must already be saved)
            GpsData gpsData = extractAndSaveGpsData(savedPhoto, uploadPath.toFile());
            if (gpsData != null) {
                savedPhoto.setGpsData(gpsData);
            }

            log.info("Photo uploaded successfully with ID: {}", savedPhoto.getId());

            return photoMapper.toPhotoDTO(savedPhoto);


        } catch (PhotoUploadException | FileStorageException e) {
            log.error("Photo upload failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during photo upload: {}", e.getMessage(), e);
            throw new PhotoUploadException("Failed to upload photo", e);
        }
    }

    /**
     * Get photo by ID
     */
    public PhotoDTO getPhotoById(Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with ID: " + id));
        return photoMapper.toPhotoDTO(photo);
    }

    /**
     * Get all photos with pagination
     */
    public Page<PhotoDTO> getAllPhotos(Pageable pageable) {
        return photoRepository.findAll(pageable).map(photoMapper::toPhotoDTO);
    }

    /**
     * Validate photo file
     */
    private void validatePhotoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new PhotoUploadException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new PhotoUploadException("Only image files are allowed");
        }

        // Check supported formats
        if (!isSupportedImageFormat(contentType)) {
            throw new PhotoUploadException("Unsupported image format. Supported: JPEG, PNG, GIF, TIFF");
        }
    }

    /**
     * Check if image format is supported
     */
    private boolean isSupportedImageFormat(String contentType) {
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/tiff");
    }

    /**
     * Generate unique filename using UUID
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * Extract EXIF data from file
     */
    private ExifData extractAndSaveExifData(Photo photo, File imageFile) {
        try {
            EXIFDataDTO exifDTO = exifExtractor.extractExifData(imageFile);
            if (exifDTO == null) {
                log.debug("No EXIF data to extract");
                return null;
            }

            ExifData exifData = new ExifData();
            exifData.setPhoto(photo);
            exifData.setCameraMake(exifDTO.getCameraMake());
            exifData.setCameraModel(exifDTO.getCameraModel());
            exifData.setIso(exifDTO.getIso());
            exifData.setAperture(exifDTO.getAperture());
            exifData.setExposureTime(exifDTO.getExposureTime());
            exifData.setFocalLength(exifDTO.getFocalLength());
            exifData.setDateTaken(exifDTO.getDateTaken());
            exifData.setOrientation(exifDTO.getOrientation());

            ExifData savedExifData = exifDataRepository.save(exifData);
            log.debug("EXIF data extracted and saved for photo: {}", photo.getId());
            return savedExifData;

        } catch (ExifExtractionException e) {
            log.warn("EXIF extraction failed, continuing without it: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error during EXIF extraction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract GPS data from EXIF and perform reverse geocoding
     */
    private GpsData extractAndSaveGpsData(Photo photo, File imageFile) {
        try {
            EXIFDataDTO exifDTO = exifExtractor.extractExifData(imageFile);
            if (exifDTO == null) {
                log.debug("No EXIF data available for GPS extraction");
                return null;
            }

            // Extract GPS from EXIF using Apache Commons Imaging
            ImageMetadata metadata = Imaging.getMetadata(imageFile);

            if (!(metadata instanceof JpegImageMetadata jpegMetadata)) {
                log.debug("No JPEG metadata available for GPS extraction");
                return null;
            }

            TiffImageMetadata exifMetadata = jpegMetadata.getExif();
            if (exifMetadata == null) {
                log.debug("No EXIF metadata available for GPS extraction");
                return null;
            }

            GPSInfo gpsInfo = exifMetadata.getGPS();
            if (gpsInfo == null) {
                log.debug("No GPS info in EXIF data");
                return null;
            }

            double latitude = gpsInfo.getLatitudeAsDegreesNorth();
            double longitude = gpsInfo.getLongitudeAsDegreesEast();

            GpsData gpsData = new GpsData();
            gpsData.setPhoto(photo);
            gpsData.setLatitude(BigDecimal.valueOf(latitude));
            gpsData.setLongitude(BigDecimal.valueOf(longitude));

            // Set altitude if available
            TiffField altitudeField = exifMetadata.findField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
            if (altitudeField != null) {
                double altitude = altitudeField.getDoubleValue();
                gpsData.setAltitude(BigDecimal.valueOf(altitude));
            }

            // Generate Google Maps link
            gpsData.setGoogleMapsLink(reverseGeocoder.generateGoogleMapsLink(gpsData.getLatitude(), gpsData.getLongitude()));

            // Perform reverse geocoding
            String[] geoData = reverseGeocoder.reverseGeocode(gpsData.getLatitude(), gpsData.getLongitude());
            gpsData.setCountry(geoData[0]);
            gpsData.setCity(geoData[1]);

            GpsData savedGpsData = gpsDataRepository.save(gpsData);
            log.debug("GPS data extracted and saved for photo: {}", photo.getId());
            return savedGpsData;

        } catch (Exception e) {
            log.warn("GPS data extraction failed, continuing without it: {}", e.getMessage());
            return null;
        }
    }
}
      