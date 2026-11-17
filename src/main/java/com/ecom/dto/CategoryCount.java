package com.ecom.dto;

/** A category and how many active products are in it. JSON: { "name": "Audio", "count": 4 } */
public record CategoryCount(String name, long count) {
}