package com.pulseai.ai.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AiSuggestion {

    public enum FeedbackStatus { PENDING, ACCEPTED, EDITED, REJECTED }

    private final UUID id;
    private final String ticketId;
    private final String suggestedResponse;
    private final ConfidenceScore confidenceScore;
    private final List<String> sourceArticleIds;
    private FeedbackStatus feedbackStatus;
    private String agentEditedResponse;
    private final Instant createdAt;
    private Instant feedbackAt;

    public static AiSuggestion create(String ticketId, String suggestedResponse,
                                       ConfidenceScore confidenceScore, List<String> sourceArticleIds) {
        return new AiSuggestion(UUID.randomUUID(), ticketId, suggestedResponse,
                confidenceScore, sourceArticleIds, FeedbackStatus.PENDING, null, Instant.now(), null);
    }

    public static AiSuggestion reconstitute(UUID id, String ticketId, String suggestedResponse,
                                             ConfidenceScore confidenceScore, List<String> sourceArticleIds,
                                             FeedbackStatus feedbackStatus, String agentEditedResponse,
                                             Instant createdAt, Instant feedbackAt) {
        return new AiSuggestion(id, ticketId, suggestedResponse, confidenceScore,
                sourceArticleIds, feedbackStatus, agentEditedResponse, createdAt, feedbackAt);
    }

    private AiSuggestion(UUID id, String ticketId, String suggestedResponse,
                          ConfidenceScore confidenceScore, List<String> sourceArticleIds,
                          FeedbackStatus feedbackStatus, String agentEditedResponse,
                          Instant createdAt, Instant feedbackAt) {
        this.id = id; this.ticketId = ticketId; this.suggestedResponse = suggestedResponse;
        this.confidenceScore = confidenceScore; this.sourceArticleIds = List.copyOf(sourceArticleIds);
        this.feedbackStatus = feedbackStatus; this.agentEditedResponse = agentEditedResponse;
        this.createdAt = createdAt; this.feedbackAt = feedbackAt;
    }

    public void accept() {
        ensurePending();
        this.feedbackStatus = FeedbackStatus.ACCEPTED;
        this.feedbackAt = Instant.now();
    }

    public void reject() {
        ensurePending();
        this.feedbackStatus = FeedbackStatus.REJECTED;
        this.feedbackAt = Instant.now();
    }

    public void editAndAccept(String editedResponse) {
        ensurePending();
        if (editedResponse == null || editedResponse.isBlank())
            throw new IllegalArgumentException("Edited response must not be blank");
        this.feedbackStatus = FeedbackStatus.EDITED;
        this.agentEditedResponse = editedResponse;
        this.feedbackAt = Instant.now();
    }

    private void ensurePending() {
        if (feedbackStatus != FeedbackStatus.PENDING)
            throw new IllegalStateException("Feedback already submitted: " + feedbackStatus);
    }

    public UUID getId()                        { return id; }
    public String getTicketId()                { return ticketId; }
    public String getSuggestedResponse()       { return suggestedResponse; }
    public ConfidenceScore getConfidenceScore(){ return confidenceScore; }
    public List<String> getSourceArticleIds()  { return sourceArticleIds; }
    public FeedbackStatus getFeedbackStatus()  { return feedbackStatus; }
    public String getAgentEditedResponse()     { return agentEditedResponse; }
    public Instant getCreatedAt()              { return createdAt; }
    public Instant getFeedbackAt()             { return feedbackAt; }
}
