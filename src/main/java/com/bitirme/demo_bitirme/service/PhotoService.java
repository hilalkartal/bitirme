package com.bitirme.demo_bitirme.service;

import com.bitirme.demo_bitirme.data.EXIFData;
import com.bitirme.demo_bitirme.data.GPSAddress;
import com.bitirme.demo_bitirme.data.GPSData;
import com.bitirme.demo_bitirme.data.Photo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PhotoService {

    public Photo savePhoto(MultipartFile file, String description) {
        try {

            //kaydedilecek klasör
            Path uploadDir = Paths.get("uploads/");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            //resim kaydı
            Path filePath = uploadDir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // photo nesnesi
            Photo photo = new Photo();
            photo.setFileName(file.getOriginalFilename());
            photo.setDescription(description);
            photo.setUrl("/uploads/" + file.getOriginalFilename());
            photo.setSize(file.getSize());
            photo.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

            // EXIF verisini oku ve Photo'a ekle
            extractAndSaveEXIFData(filePath.toFile(), photo);

            return photo;

        } catch (IOException e) {
            throw new RuntimeException("Fotoğraf yüklenemedi", e);
        }
    }

    private void extractAndSaveEXIFData(File imgFile, Photo photo) {
        try {
            ImageMetadata metadata = Imaging.getMetadata(imgFile);
            if (!(metadata instanceof JpegImageMetadata jpegMetadata)) {
                return; // EXIF yok veya JPEG değil
            }

            EXIFData exifData = new EXIFData();
            GPSData gpsData = null;

            TiffImageMetadata exifMetadata = jpegMetadata.getExif();
            if (exifMetadata != null) {
                // GPS bilgisi
                TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
                if (gpsInfo != null) {
                    double longitude = gpsInfo.getLongitudeAsDegreesEast();
                    double latitude = gpsInfo.getLatitudeAsDegreesNorth();

                    gpsData = new GPSData();
                    gpsData.setLongitude(longitude);
                    gpsData.setLatitude(latitude);
                    gpsData.setAltitude(null);
                    gpsData.setTimestamp(LocalDateTime.now());
                    String url = "https://www.google.com/maps/search/?api=1&query="
                            + latitude + "," + longitude;
                    gpsData.setGoogleMapsLink(url);
                    GPSAddress address = reverseGeocode(latitude, longitude);
                    gpsData.setAddress(address);
                    System.out.println(gpsData);
                }
            }

            exifData.setGpsData(gpsData);

            // Diğer alanlar şimdilik null
            exifData.setMake(null);
            exifData.setModel(null);
            exifData.setSoftware(null);
            exifData.setDateTaken(null);
            exifData.setOrientation(0);
            exifData.setExposureTime(0.0);
            exifData.setAperture(0.0);
            exifData.setIso(0);
            exifData.setFocalLength(0.0);
            exifData.setFlashUsed(false);

            photo.setExifData(exifData);

            System.out.println(photo.getExifData());
        } catch (ImageReadException | IOException e) {
            System.err.println("EXIF okunamadı: " + e.getMessage());
        }
    }

    private GPSAddress reverseGeocode(double latitude, double longitude) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat="
                    + latitude + "&lon=" + longitude;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "demo-bitirme-app/1.0 (hilal.itu.icin@gmail.com)")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode addressNode = root.get("address");
            if (addressNode == null || addressNode.isNull()) return null;

            // JSON'daki `addresi maple
            GPSAddress gpsAddress = mapper.treeToValue(addressNode, GPSAddress.class);

            return gpsAddress;
        } catch (Exception e) {
            return null;
        }
    }
}
