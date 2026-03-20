package com.pulseai.ai.infrastructure.web;

import com.pulseai.ai.application.dto.KnowledgeArticleResponse;
import com.pulseai.ai.application.dto.SuggestionResponse;
import com.pulseai.ai.application.service.AiApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiApplicationService aiService;

    public AiController(AiApplicationService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/suggestions/ticket/{ticketId}")
    public SuggestionResponse getSuggestion(@PathVariable String ticketId) {
        return aiService.getSuggestionByTicketId(ticketId);
    }

    @PostMapping("/suggestions/{id}/accept")
    public SuggestionResponse accept(@PathVariable UUID id) {
        return aiService.acceptSuggestion(id);
    }

    @PostMapping("/suggestions/{id}/reject")
    public SuggestionResponse reject(@PathVariable UUID id) {
        return aiService.rejectSuggestion(id);
    }

    @PostMapping("/suggestions/{id}/edit")
    public SuggestionResponse edit(@PathVariable UUID id,
                                   @Valid @RequestBody EditRequest request) {
        return aiService.editSuggestion(id, request.editedResponse());
    }

    @GetMapping("/articles")
    public java.util.List<KnowledgeArticleResponse> getArticles() {
        return aiService.getAllArticles();
    }

    @PostMapping("/articles")
    @ResponseBody
    public void createArticle(@Valid @RequestBody CreateArticleRequest request) {
        aiService.createAndIndexArticle(request.title(), request.content(), request.category());
    }

    public record EditRequest(@NotBlank String editedResponse) {}
    public record CreateArticleRequest(@NotBlank String title, @NotBlank String content, String category) {}
}
