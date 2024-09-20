package org.example.chatserversem3demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatController {

    // Liste til at gemme alle beskeder sendt under sessionen
    private final List<String> chatHistory = new ArrayList<>();

    // Liste over SseEmitters for at sende beskeder til alle tilsluttede klienter
    private final List<SseEmitter> emitters = new ArrayList<>();

    @GetMapping("/")
    public String chatPage() {
        // Returnerer chat.html når "/" kaldes
        return "chat";
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        // Opretter en SseEmitter for at sende events til klienterne (Server-Sent Events)
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);

        // Send den eksisterende chathistorik til den nye klient
        try {
            for (String message : chatHistory) {
                emitter.send(SseEmitter.event().data(message));
            }
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        // Fjern emitteren, når klienten afbryder forbindelsen
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam String message, Model model) {
        // Tilføj beskeden til chathistorikken
        chatHistory.add(message);

        // Liste til at gemme døde emitters (klienter, der er afbrudt)
        List<SseEmitter> deadEmitters = new ArrayList<>();

        // Send beskeden til alle tilsluttede klienter
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        // Fjern døde emitters
        emitters.removeAll(deadEmitters);

        model.addAttribute("message", message);
        return "redirect:/";
    }
}
