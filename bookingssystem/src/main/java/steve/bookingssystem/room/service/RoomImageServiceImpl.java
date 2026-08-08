package steve.bookingssystem.room.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.RoomImage;
import steve.bookingssystem.room.model.RoomImageDto;
import steve.bookingssystem.room.repository.RoomImageRepository;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.storage.StorageService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomImageServiceImpl implements RoomImageService {

    @Autowired
    private RoomImageRepository roomImageRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public List<RoomImageDto> list(UUID roomId) {
        return roomImageRepository.findByRoom_IdOrderByPositionAsc(roomId)
                .stream().map(RoomImageDto::from).toList();
    }

    @Override
    public RoomImageDto upload(UUID roomId, MultipartFile file) {
        authorizationService.requireAdmin();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        String url = storageService.uploadRoomImage(roomId, file);
        int position = (int) roomImageRepository.countByRoom_Id(roomId);

        RoomImage image = new RoomImage();
        image.setRoom(room);
        image.setImageUrl(url);
        image.setPosition(position);
        roomImageRepository.save(image);

        if (position == 0) {
            room.setImageUrl(url);
            roomRepository.save(room);
        }

        return RoomImageDto.from(image);
    }

    @Override
    public void delete(UUID roomId, UUID imageId) {
        authorizationService.requireAdmin();
        RoomImage toDelete = roomImageRepository.findByIdAndRoom_Id(imageId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        roomImageRepository.delete(toDelete);

        List<RoomImage> remaining = roomImageRepository.findByRoom_IdOrderByPositionAsc(roomId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        roomImageRepository.saveAll(remaining);

        syncRoomCover(roomId, remaining);
    }

    @Override
    public List<RoomImageDto> reorder(UUID roomId, List<UUID> orderedImageIds) {
        authorizationService.requireAdmin();
        List<RoomImage> images = roomImageRepository.findByRoom_IdOrderByPositionAsc(roomId);

        Map<UUID, RoomImage> byId = new LinkedHashMap<>();
        for (RoomImage image : images) {
            byId.put(image.getId(), image);
        }

        if (orderedImageIds.size() != images.size() || !byId.keySet().containsAll(orderedImageIds)) {
            throw new IllegalArgumentException("Reihenfolge muss genau die vorhandenen Bilder enthalten");
        }

        for (int i = 0; i < orderedImageIds.size(); i++) {
            byId.get(orderedImageIds.get(i)).setPosition(i);
        }
        List<RoomImage> reordered = orderedImageIds.stream().map(byId::get).toList();
        roomImageRepository.saveAll(reordered);

        syncRoomCover(roomId, reordered);

        return list(roomId);
    }

    private void syncRoomCover(UUID roomId, List<RoomImage> currentOrder) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        room.setImageUrl(currentOrder.isEmpty() ? null : currentOrder.get(0).getImageUrl());
        roomRepository.save(room);
    }
}
