package com.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Address {
    private Long id;
    private String street;
    private String city;
    private String state;
    private String country;
    private String zipcode;
}
