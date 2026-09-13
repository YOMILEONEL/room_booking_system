package steve.bookingssystem.assistant.Controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.assistant.model.AssistantMessageDTO;
import steve.bookingssystem.assistant.model.AssistantMessageRequest;
import steve.bookingssystem.assistant.service.AssistantMessageService;

import java.util.List;

@RestController
@RequestMapping("/assistant")
public class AssistantMessageController {

    @Autowired
    private AssistantMessageService assistantMessageService;

    @GetMapping("/history")
    public List<AssistantMessageDTO> getHistory() {
        return assistantMessageService.getHistory();
    }

    @PostMapping("/history")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssistantMessageDTO> addMessages(@Valid @RequestBody List<@Valid AssistantMessageRequest> messages) {
        return assistantMessageService.addMessages(messages);
    }

}
