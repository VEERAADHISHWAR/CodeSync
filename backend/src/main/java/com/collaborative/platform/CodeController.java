
package com.collaborative.platform;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CodeController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;
    private final CodeVersionRepository codeVersionRepository;

    public CodeController(
            SimpMessagingTemplate messagingTemplate,
            RoomRepository roomRepository,
            CodeVersionRepository codeVersionRepository) {

        this.messagingTemplate = messagingTemplate;
        this.roomRepository = roomRepository;
        this.codeVersionRepository = codeVersionRepository;
    }

    // Receive code changes from users
    @MessageMapping("/code/{roomId}")
    public void receiveCode(
            @DestinationVariable String roomId,
            CodeMessage message) {

        System.out.println("Received code for room: " + roomId);

        // Save the latest code in MySQL
        roomRepository.updateCodeAndLanguage(
                roomId,
                message.getCode(),
                message.getLanguage()
        );

        // Broadcast the code to everyone in the room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                message
        );

        System.out.println("Code saved and broadcast successfully!");
    }

    // Save a manual snapshot with optional name and description
    @MessageMapping("/version/{roomId}")
    public void saveVersion(
            @DestinationVariable String roomId,
            CodeMessage message) {

        System.out.println("Saving version for room: " + roomId);

        boolean saved = codeVersionRepository.saveVersion(
                roomId,
                message.getCode(),
                message.getLanguage(),
                message.getVersionName(),
                message.getDescription()
        );

        if (saved) {
            System.out.println("Version saved successfully!");
        } else {
            System.out.println("Duplicate version skipped.");
        }
    }

    // Restore a historical version to the shared room
    @MessageMapping("/restore/{roomId}")
    public void restoreVersion(
            @DestinationVariable String roomId,
            CodeMessage message) {

        System.out.println("Restoring version for room: " + roomId);

        // Update the current code in the rooms table
        roomRepository.updateCodeAndLanguage(
                roomId,
                message.getCode(),
                message.getLanguage()
        );

        // Save the restored code as a new historical version
        codeVersionRepository.saveVersion(
                roomId,
                message.getCode(),
                message.getLanguage(),
                "Restored Version",
                "Code restored from version history"
        );

        // Broadcast the restored code to all collaborators
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                message
        );

        System.out.println("Version restored successfully!");
    }
}
