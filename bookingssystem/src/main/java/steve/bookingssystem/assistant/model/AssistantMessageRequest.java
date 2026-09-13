package steve.bookingssystem.assistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssistantMessageRequest {

    @NotNull
    private AssistantMessageRole role;

    @NotBlank
    // A pathological reply (or a client bug looping) shouldn't be able to grow a single row
    // without bound - matches the question-length cap already enforced in api/assistant/route.ts.
    @Size(max = 4000)
    private String content;
}
