package com.xueying.jobapplicationtracker.dto;

public class CompanyAutoFillResponse {
    private String summary;
    private String website;
    private String country;
    private String suggestedRegion;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSuggestedRegion() {
        return suggestedRegion;
    }

    public void setSuggestedRegion(String suggestedRegion) {
        this.suggestedRegion = suggestedRegion;
    }
}

