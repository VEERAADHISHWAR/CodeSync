
package com.collaborative.platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class PresenceController {

    private final SimpMessagingTemplate messagingTemplate;

    // Room ID -> Session ID -> Participant
    private final Map<String, Map<String, Participant>> rooms =
            new ConcurrentHashMap<>();

    // WebSocket Session ID -> Room ID
    private final Map<String, String> sessionRooms =
            new ConcurrentHashMap<>();

    public PresenceController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public record PresenceMessage(
            String participantId,
            String displayName,
            String action
    ) {}

    public record Participant(
            String participantId,
            String displayName
    ) {}

    // Return the current participant list to a newly subscribed client
    @SubscribeMapping("/presence/{roomId}")
    public List<Participant> getCurrentParticipants(
            @DestinationVariable String roomId) {

        Map<String, Participant> participants = rooms.get(roomId);

        if (participants == null) {
            return List.of();
        }

        synchronized (participants) {
            return List.copyOf(participants.values());
        }
    }

    // Handle explicit JOIN and LEAVE messages
    @MessageMapping("/presence/{roomId}")
    public void updatePresence(
            @DestinationVariable String roomId,
            @Payload PresenceMessage message,
            @Header("simpSessionId") String sessionId) {

        if (message == null ||
                sessionId == null ||
                message.participantId() == null ||
                message.displayName() == null ||
                message.action() == null) {
            return;
        }

        if ("JOIN".equals(message.action())) {

            String previousRoom = sessionRooms.put(sessionId, roomId);

            if (previousRoom != null && !previousRoom.equals(roomId)) {
                removeParticipant(previousRoom, sessionId);
            }

            Map<String, Participant> participants =
                    rooms.computeIfAbsent(
                            roomId,
                            key -> new ConcurrentHashMap<>()
                    );

            synchronized (participants) {

                participants.put(
                        sessionId,
                        new Participant(
                                message.participantId(),
                                message.displayName()
                        )
                );

                broadcastParticipants(roomId, participants);
            }

            System.out.println(
                    message.displayName() + " joined room " + roomId
            );

        } else if ("LEAVE".equals(message.action())) {

            sessionRooms.remove(sessionId, roomId);

            removeParticipant(roomId, sessionId);

            System.out.println(
                    message.displayName() + " left room " + roomId
            );
        }
    }

    // Handle unexpected browser closure or WebSocket disconnection
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        String sessionId = event.getSessionId();

        if (sessionId != null) {

            String roomId = sessionRooms.remove(sessionId);

            if (roomId != null) {

                removeParticipant(roomId, sessionId);

                System.out.println(
                        "WebSocket disconnected. Session: "
                                + sessionId + ", Room: " + roomId
                );
            }
        }
    }

    // Remove a participant and broadcast the updated list
    private void removeParticipant(String roomId, String sessionId) {

        Map<String, Participant> participants = rooms.get(roomId);

        if (participants != null) {

            synchronized (participants) {

                Participant removed = participants.remove(sessionId);

                if (removed != null) {

                    broadcastParticipants(roomId, participants);

                    System.out.println(
                            removed.displayName()
                                    + " removed from room " + roomId
                    );
                }

                if (participants.isEmpty()) {
                    rooms.remove(roomId, participants);
                }
            }
        }
    }

    // Broadcast the current participant list to everyone in the room
    private void broadcastParticipants(
            String roomId,
            Map<String, Participant> participants) {

        List<Participant> currentParticipants =
                List.copyOf(participants.values());

        messagingTemplate.convertAndSend(
                "/topic/presence/" + roomId,
                currentParticipants
        );
    }
}
