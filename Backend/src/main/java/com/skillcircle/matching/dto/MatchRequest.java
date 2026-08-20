package com.skillcircle.matching.dto;

import lombok.Data;

/**
 * Request body for finding matches.
 */
@Data
public class MatchRequest {

    private Integer limit;          // Max results (default: 10)
    private String goalType;        // Optional goal type filter
    private String availability;    // Optional availability filter
    private Integer timezoneOverlapHours; // Optional minimum timezone overlap
}
