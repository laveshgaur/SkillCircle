package com.skillcircle.ai.service;

import com.skillcircle.auth.entity.User;
import com.skillcircle.auth.repository.UserRepository;
import com.skillcircle.community.entity.Message;
import com.skillcircle.community.entity.Thread;
import com.skillcircle.community.repository.MessageRepository;
import com.skillcircle.community.repository.ThreadRepository;
import com.skillcircle.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI-powered thread summarization service.
 *
 * - Manual trigger: POST /threads/{id}/summarize
 * - Auto trigger: called when a thread exceeds 50 messages
 * - Generates a concise markdown summary using GPT-4o-mini
 * - Stores the summary in the thread's ai_summary column
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadSummarizationService {

    private static final int AUTO_SUMMARY_THRESHOLD = 50;

    private static final String SYSTEM_PROMPT = """
            You are a concise technical discussion summarizer for a developer collaboration platform.
            Given a conversation thread, produce a clear, structured markdown summary that captures:
            1. **Topic**: What the discussion is about
            2. **Key Points**: The main decisions, insights, or conclusions (bullet points)
            3. **Action Items**: Any tasks or follow-ups mentioned
            4. **Participants**: Who contributed significantly
            
            Keep the summary under 300 words. Use markdown formatting.
            If the conversation is too short or trivial, say "Not enough content to summarize."
            """;

    private final LLMClient llmClient;
    private final ThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * Summarize a thread on demand.
     *
     * @param threadId the thread to summarize
     * @return the generated summary markdown
     */
    @Transactional
    public String summarizeThread(UUID threadId) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread", "id", threadId));

        List<Message> messages = messageRepository.findTop50ByThreadIdOrderByCreatedAtDesc(threadId);

        if (messages.isEmpty()) {
            return "No messages to summarize.";
        }

        // Build conversation text
        String conversationText = buildConversationText(messages, thread.getTitle());

        // Generate summary via LLM
        String summary;
        if (llmClient.isAvailable()) {
            summary = llmClient.chatCompletion(SYSTEM_PROMPT, conversationText);
        } else {
            summary = generateFallbackSummary(messages, thread.getTitle());
        }

        if (summary.isBlank()) {
            summary = generateFallbackSummary(messages, thread.getTitle());
        }

        // Persist summary
        thread.setAiSummary(summary);
        thread.setSummaryUpdatedAt(Instant.now());
        threadRepository.save(thread);

        log.info("Thread {} summarized ({} messages → {} chars)",
                threadId, messages.size(), summary.length());

        return summary;
    }

    /**
     * Check if a thread should be auto-summarized (>50 messages).
     * Called after each message send.
     *
     * <p>Runs on the {@code aiTaskExecutor} so the (potentially slow) LLM call
     * never blocks the chat/WebSocket thread. Intentionally not {@code @Transactional}:
     * {@link #summarizeThread} persists via a single {@code save()}, and we do not
     * want to hold a DB connection open across the network call to the LLM.
     *
     * @param threadId the thread to check
     */
    @Async("aiTaskExecutor")
    public void checkAutoSummarize(UUID threadId) {
        long messageCount = messageRepository.countByThreadId(threadId);

        if (messageCount >= AUTO_SUMMARY_THRESHOLD) {
            Thread thread = threadRepository.findById(threadId).orElse(null);
            if (thread == null) return;

            // Only auto-summarize if no recent summary or if it's stale (>1h old)
            if (thread.getSummaryUpdatedAt() == null ||
                    thread.getSummaryUpdatedAt().isBefore(Instant.now().minusSeconds(3600))) {
                log.info("Auto-summarizing thread {} ({} messages)", threadId, messageCount);
                summarizeThread(threadId);
            }
        }
    }

    /**
     * Build conversation text from messages for the LLM prompt.
     */
    String buildConversationText(List<Message> messages, String threadTitle) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread: ").append(threadTitle != null ? threadTitle : "Untitled").append("\n\n");

        // Reverse to chronological order (messages are fetched newest-first)
        List<Message> chronological = new java.util.ArrayList<>(messages);
        Collections.reverse(chronological);

        for (Message msg : chronological) {
            String username = userRepository.findById(msg.getSenderId())
                    .map(User::getUsername).orElse("user");
            sb.append("[").append(username).append("]: ").append(msg.getContent()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Generate a simple fallback summary when LLM is unavailable.
     */
    String generateFallbackSummary(List<Message> messages, String title) {
        long uniqueParticipants = messages.stream()
                .map(Message::getSenderId).distinct().count();

        return String.format("""
                ## Thread Summary
                
                **Topic**: %s
                
                - **Messages**: %d
                - **Participants**: %d
                - **Period**: %s to %s
                
                *AI summary unavailable — this is an auto-generated placeholder.*
                """,
                title != null ? title : "Discussion",
                messages.size(),
                uniqueParticipants,
                messages.get(messages.size() - 1).getCreatedAt(),
                messages.get(0).getCreatedAt()
        );
    }
}
