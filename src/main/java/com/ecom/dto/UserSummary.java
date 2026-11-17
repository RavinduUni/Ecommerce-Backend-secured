package com.ecom.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The few user fields other responses need (cart lines, orders).
 * Replaces the full User object, which also carried phone, role, address and dates.
 */
@Data
@NoArgsConstructor
public class UserSummary {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}