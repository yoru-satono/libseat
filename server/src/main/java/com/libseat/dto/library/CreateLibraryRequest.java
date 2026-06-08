package com.libseat.dto.library;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateLibraryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String address;

    @Size(max = 500)
    private String logoUrl;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
