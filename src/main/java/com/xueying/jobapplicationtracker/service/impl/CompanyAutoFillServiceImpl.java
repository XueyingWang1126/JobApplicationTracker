package com.xueying.jobapplicationtracker.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xueying.jobapplicationtracker.dto.CompanyAutoFillResponse;
import com.xueying.jobapplicationtracker.service.CompanyAutoFillService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Calls public Wikipedia/Wikidata APIs and caches responses for form auto-fill.
 */
@Service
public class CompanyAutoFillServiceImpl implements CompanyAutoFillService {
    private static final String UNKNOWN = "N/A";
    private static final long CACHE_TTL_MILLIS = Duration.ofHours(6).toMillis();
    private static final long FALLBACK_CACHE_TTL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final Set<String> EU_COUNTRIES = new HashSet<>(Arrays.asList(
            "austria", "belgium", "bulgaria", "croatia", "cyprus", "czech republic",
            "denmark", "estonia", "finland", "france", "germany", "greece", "hungary",
            "ireland", "italy", "latvia", "lithuania", "luxembourg", "malta",
            "netherlands", "poland", "portugal", "romania", "slovakia", "slovenia",
            "spain", "sweden"
    ));

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache;

    public CompanyAutoFillServiceImpl() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .callTimeout(4, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public CompanyAutoFillResponse autoFill(String companyName) {
        String normalized = normalize(companyName);
        if (normalized.isEmpty()) {
            return fallback();
        }

        String key = normalized.toLowerCase();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expireAtMillis > System.currentTimeMillis()) {
            return copy(cached.response);
        }

        CompanyAutoFillResponse response = fallback();
        try {
            enrichFromWikipedia(normalized, response);
        } catch (Exception ignored) {
        }
        response.setSuggestedRegion(suggestRegion(response.getCountry(), response.getSummary()));

        long ttl = isMeaningful(response) ? CACHE_TTL_MILLIS : FALLBACK_CACHE_TTL_MILLIS;
        cache.put(key, new CacheEntry(copy(response), System.currentTimeMillis() + ttl));
        return response;
    }

    private void enrichFromWikipedia(String companyName, CompanyAutoFillResponse target) throws Exception {
        String encoded = UriUtils.encodePathSegment(companyName, StandardCharsets.UTF_8);
        String summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded;
        JsonNode summaryRoot = fetchJson(summaryUrl);
        if (summaryRoot == null) {
            return;
        }

        String summary = readText(summaryRoot, "extract");
        if (isPresent(summary)) {
            target.setSummary(summary);
        }

        String wikiPage = readNestedText(summaryRoot, "content_urls", "desktop", "page");
        if (isPresent(wikiPage)) {
            target.setWebsite(wikiPage);
        }

        String wikibaseItem = readText(summaryRoot, "wikibase_item");
        if (isPresent(wikibaseItem)) {
            enrichFromWikidata(wikibaseItem, target);
        }
    }

    private void enrichFromWikidata(String itemId, CompanyAutoFillResponse target) throws Exception {
        String dataUrl = "https://www.wikidata.org/wiki/Special:EntityData/" + itemId + ".json";
        JsonNode root = fetchJson(dataUrl);
        if (root == null) {
            return;
        }

        JsonNode entity = root.path("entities").path(itemId);
        if (entity.isMissingNode()) {
            return;
        }

        String officialWebsite = readClaimText(entity, "P856");
        if (isPresent(officialWebsite)) {
            target.setWebsite(officialWebsite);
        }

        String countryId = readClaimEntityId(entity, "P17");
        if (isPresent(countryId)) {
            String country = resolveCountryName(countryId);
            if (isPresent(country)) {
                target.setCountry(country);
            }
        }
    }

    private String resolveCountryName(String countryId) throws Exception {
        String countryUrl = "https://www.wikidata.org/wiki/Special:EntityData/" + countryId + ".json";
        JsonNode countryRoot = fetchJson(countryUrl);
        if (countryRoot == null) {
            return UNKNOWN;
        }
        JsonNode countryEntity = countryRoot.path("entities").path(countryId);
        String label = readNestedText(countryEntity, "labels", "en", "value");
        return isPresent(label) ? label : UNKNOWN;
    }

    private JsonNode fetchJson(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "JobTracker/1.0 (local)")
                .header("Api-User-Agent", "JobTracker/1.0 (local)")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    private String readClaimText(JsonNode entityNode, String claimKey) {
        JsonNode valueNode = firstClaimValue(entityNode, claimKey);
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return UNKNOWN;
        }
        if (valueNode.isTextual()) {
            return valueNode.asText();
        }
        return UNKNOWN;
    }

    private String readClaimEntityId(JsonNode entityNode, String claimKey) {
        JsonNode valueNode = firstClaimValue(entityNode, claimKey);
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return UNKNOWN;
        }
        String id = readText(valueNode, "id");
        return isPresent(id) ? id : UNKNOWN;
    }

    private JsonNode firstClaimValue(JsonNode entityNode, String claimKey) {
        JsonNode claimArray = entityNode.path("claims").path(claimKey);
        if (!claimArray.isArray() || claimArray.size() == 0) {
            return null;
        }
        return claimArray.get(0)
                .path("mainsnak")
                .path("datavalue")
                .path("value");
    }

    private String suggestRegion(String country, String summary) {
        String lowerCountry = normalize(country).toLowerCase();
        String lowerSummary = normalize(summary).toLowerCase();

        if (containsAny(lowerCountry, "united kingdom", "uk", "england", "scotland", "wales", "northern ireland")
                || containsAny(lowerSummary, "headquartered in the united kingdom", "british")) {
            return "UK";
        }
        if (containsAny(lowerCountry, "china", "hong kong", "macau", "taiwan")
                || containsAny(lowerSummary, "chinese")) {
            return "China";
        }
        if (EU_COUNTRIES.contains(lowerCountry)
                || containsAny(lowerSummary, "european union", "eu-based", "european company")) {
            return "EU";
        }
        return "Other";
    }

    private boolean containsAny(String value, String... tokens) {
        if (!isPresent(value)) {
            return false;
        }
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String readText(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return UNKNOWN;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || !value.isTextual()) {
            return UNKNOWN;
        }
        return value.asText();
    }

    private String readNestedText(JsonNode node, String... fields) {
        JsonNode current = node;
        for (String field : fields) {
            if (current == null || current.isMissingNode()) {
                return UNKNOWN;
            }
            current = current.path(field);
        }
        if (current == null || current.isMissingNode() || current.isNull() || !current.isTextual()) {
            return UNKNOWN;
        }
        return current.asText();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty() && !UNKNOWN.equalsIgnoreCase(value.trim());
    }

    private CompanyAutoFillResponse fallback() {
        CompanyAutoFillResponse response = new CompanyAutoFillResponse();
        response.setSummary(UNKNOWN);
        response.setWebsite(UNKNOWN);
        response.setCountry(UNKNOWN);
        response.setSuggestedRegion("Other");
        return response;
    }

    private CompanyAutoFillResponse copy(CompanyAutoFillResponse source) {
        CompanyAutoFillResponse response = new CompanyAutoFillResponse();
        response.setSummary(source.getSummary());
        response.setWebsite(source.getWebsite());
        response.setCountry(source.getCountry());
        response.setSuggestedRegion(source.getSuggestedRegion());
        return response;
    }

    private boolean isMeaningful(CompanyAutoFillResponse response) {
        return isPresent(response.getSummary()) || isPresent(response.getWebsite()) || isPresent(response.getCountry());
    }

    private static class CacheEntry {
        private final CompanyAutoFillResponse response;
        private final long expireAtMillis;

        private CacheEntry(CompanyAutoFillResponse response, long expireAtMillis) {
            this.response = response;
            this.expireAtMillis = expireAtMillis;
        }
    }
}

