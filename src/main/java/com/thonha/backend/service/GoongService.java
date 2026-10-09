package com.thonha.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gọi các dịch vụ REST của Goong bằng GOONG_API_KEY (khóa API phía backend).
 * Khóa vẽ bản đồ (Maptiles key) là khóa khác, chỉ dùng ở frontend.
 */
@Service
@Slf4j
public class GoongService {
    private static final String BASE_URL = "https://rsapi.goong.io";

    private final RestClient http = RestClient.create();
    private final String apiKey;

    public GoongService(@Value("${app.goong.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Gọi Goong Distance Matrix API để tính khoảng cách và thời gian.
     */
    @SuppressWarnings("unchecked")
    public DistanceMatrixResponse getDistanceMatrix(List<Coordinate> origins,
                                                    List<Coordinate> destinations,
                                                    String vehicle) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOONG_API_KEY chưa được cấu hình");
        }
        try {
            String originsStr = origins.stream().map(c -> c.lat + "," + c.lng).collect(Collectors.joining("|"));
            String destinationsStr = destinations.stream().map(c -> c.lat + "," + c.lng).collect(Collectors.joining("|"));
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/v2/distancematrix")
                    .queryParam("origins", "{o}")
                    .queryParam("destinations", "{d}")
                    .queryParam("vehicle", "{v}")
                    .queryParam("api_key", "{k}")
                    .encode()
                    .buildAndExpand(originsStr, destinationsStr, toGoongVehicle(vehicle), apiKey)
                    .toUri();

            Map<String, Object> root = http.get().uri(uri).retrieve().body(Map.class);
            if (root == null) {
                throw new IllegalStateException("Empty response from Goong API");
            }
            return parseResponse(root);
        } catch (Exception e) {
            log.error("Error calling Goong Distance Matrix API", e);
            throw new IllegalStateException("Failed to call Goong Distance Matrix API: " + e.getMessage(), e);
        }
    }

    /** Goong dùng car | bike | taxi | truck | hd; "motorcycle" của MatchingService tương ứng với bike. */
    private static String toGoongVehicle(String vehicle) {
        if (vehicle == null || vehicle.isBlank() || "motorcycle".equalsIgnoreCase(vehicle)) {
            return "bike";
        }
        return vehicle;
    }

    @SuppressWarnings("unchecked")
    private DistanceMatrixResponse parseResponse(Map<String, Object> root) {
        DistanceMatrixResponse response = new DistanceMatrixResponse();
        List<DistanceMatrixResponse.Row> rows = new ArrayList<>();
        if (root.get("rows") instanceof List<?> rowsNode) {
            for (Object rowObj : rowsNode) {
                DistanceMatrixResponse.Row row = new DistanceMatrixResponse.Row();
                List<DistanceMatrixResponse.Element> elements = new ArrayList<>();
                if (rowObj instanceof Map<?, ?> rowMap && rowMap.get("elements") instanceof List<?> elementsNode) {
                    for (Object elObj : elementsNode) {
                        if (!(elObj instanceof Map<?, ?> el)) continue;
                        DistanceMatrixResponse.Element element = new DistanceMatrixResponse.Element();
                        element.setStatus(String.valueOf(el.get("status")));
                        if (el.get("distance") instanceof Map<?, ?> d) {
                            DistanceMatrixResponse.Distance distance = new DistanceMatrixResponse.Distance();
                            distance.setText(d.get("text") == null ? null : String.valueOf(d.get("text")));
                            distance.setValue(d.get("value") instanceof Number n ? n.longValue() : 0L);
                            element.setDistance(distance);
                        }
                        if (el.get("duration") instanceof Map<?, ?> d) {
                            DistanceMatrixResponse.Duration duration = new DistanceMatrixResponse.Duration();
                            duration.setText(d.get("text") == null ? null : String.valueOf(d.get("text")));
                            duration.setValue(d.get("value") instanceof Number n ? n.longValue() : 0L);
                            element.setDuration(duration);
                        }
                        elements.add(element);
                    }
                }
                row.setElements(elements);
                rows.add(row);
            }
        }
        response.setRows(rows);
        return response;
    }

    // Inner DTOs
    public static class Coordinate {
        public double lat;
        public double lng;

        public Coordinate() {}

        public Coordinate(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public static Coordinate from(BigDecimal lat, BigDecimal lng) {
            if (lat == null || lng == null) return null;
            return new Coordinate(lat.doubleValue(), lng.doubleValue());
        }
    }

    public static class DistanceMatrixResponse {
        private List<Row> rows;

        public List<Row> getRows() { return rows; }
        public void setRows(List<Row> rows) { this.rows = rows; }

        public static class Row {
            private List<Element> elements;

            public List<Element> getElements() { return elements; }
            public void setElements(List<Element> elements) { this.elements = elements; }
        }

        public static class Element {
            private String status;
            private Distance distance;
            private Duration duration;

            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
            public Distance getDistance() { return distance; }
            public void setDistance(Distance distance) { this.distance = distance; }
            public Duration getDuration() { return duration; }
            public void setDuration(Duration duration) { this.duration = duration; }
        }

        public static class Distance {
            private String text;
            private long value;

            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
            public long getValue() { return value; }
            public void setValue(long value) { this.value = value; }
        }

        public static class Duration {
            private String text;
            private long value;

            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
            public long getValue() { return value; }
            public void setValue(long value) { this.value = value; }
        }
    }
}