package com.skillcircle.matching.controller;

import com.skillcircle.auth.entity.User;
import com.skillcircle.common.dto.ApiResponse;
import com.skillcircle.matching.dto.MatchRequest;
import com.skillcircle.matching.dto.MatchResponse;
import com.skillcircle.matching.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the AI Matching Engine.
 */
@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
@Tag(name = "Matching", description = "AI-powered collaborator matching")
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/find")
    @Operation(summary = "Find collaborator matches using the 3-stage AI pipeline")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> findMatches(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) MatchRequest request) {
        if (request == null) request = new MatchRequest();
        List<MatchResponse> matches = matchService.findMatches(user, request);
        return ResponseEntity.ok(ApiResponse.success(
                matches.size() + " matches found", matches));
    }

    @GetMapping("/history")
    @Operation(summary = "Get match history")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getHistory(
            @AuthenticationPrincipal User user) {
        List<MatchResponse> history = matchService.getMatchHistory(user.getId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @PostMapping("/{matchId}/accept")
    @Operation(summary = "Accept a match")
    public ResponseEntity<ApiResponse<MatchResponse>> acceptMatch(
            @AuthenticationPrincipal User user,
            @PathVariable UUID matchId) {
        MatchResponse match = matchService.acceptMatch(user.getId(), matchId);
        return ResponseEntity.ok(ApiResponse.success("Match accepted", match));
    }

    @PostMapping("/{matchId}/dismiss")
    @Operation(summary = "Dismiss a match")
    public ResponseEntity<ApiResponse<MatchResponse>> dismissMatch(
            @AuthenticationPrincipal User user,
            @PathVariable UUID matchId) {
        MatchResponse match = matchService.dismissMatch(user.getId(), matchId);
        return ResponseEntity.ok(ApiResponse.success("Match dismissed", match));
    }
}
