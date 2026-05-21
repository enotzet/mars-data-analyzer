package app.service;

import java.util.List;

public interface NasaApiAdapter {

    List<ImageRecord> fetchNasaImageLibrary();

    List<ImageRecord> fetchMarsRoverPhotos();

    byte[] downloadImage(String imageUrl);

    record ImageRecord(
            String imageUrl,
            String source,
            String title,
            String description,
            String externalId
    ) {}
}
