package com.agri.claim.ai.util;

import com.agri.claim.ai.service.FlightRouteService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WktUtil {

    private static final Pattern WKT_PATTERN = Pattern.compile(
            "POLYGON\\s*\\(\\(([^)]+)\\)\\)",
            Pattern.CASE_INSENSITIVE
    );

    public static List<FlightRouteService.LatLng> parsePolygon(String wkt) {
        if (wkt == null || wkt.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = WKT_PATTERN.matcher(wkt.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("无效的WKT多边形格式: " + wkt);
        }

        String pointsStr = matcher.group(1).trim();
        String[] pointPairs = pointsStr.split(",");

        List<FlightRouteService.LatLng> vertices = new ArrayList<>();
        for (String pair : pointPairs) {
            String[] parts = pair.trim().split("\\s+");
            if (parts.length >= 2) {
                double lon = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                vertices.add(new FlightRouteService.LatLng(lat, lon));
            }
        }

        if (vertices.size() >= 2) {
            FlightRouteService.LatLng first = vertices.get(0);
            FlightRouteService.LatLng last = vertices.get(vertices.size() - 1);
            if (Math.abs(first.lat - last.lat) < 1e-9 && Math.abs(first.lon - last.lon) < 1e-9) {
                vertices.remove(vertices.size() - 1);
            }
        }

        return vertices;
    }

    public static String toWktPolygon(List<FlightRouteService.LatLng> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("POLYGON((");
        for (int i = 0; i < vertices.size(); i++) {
            if (i > 0) sb.append(",");
            FlightRouteService.LatLng p = vertices.get(i);
            sb.append(String.format("%.6f %.6f", p.lon, p.lat));
        }
        FlightRouteService.LatLng first = vertices.get(0);
        sb.append(String.format(",%.6f %.6f))", first.lon, first.lat));
        return sb.toString();
    }

    public static double[] calculateCenter(List<FlightRouteService.LatLng> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return new double[]{0, 0};
        }
        double sumLon = 0, sumLat = 0;
        for (FlightRouteService.LatLng p : vertices) {
            sumLon += p.lon;
            sumLat += p.lat;
        }
        return new double[]{sumLon / vertices.size(), sumLat / vertices.size()};
    }

    public static BigDecimal calculateCenterLon(List<FlightRouteService.LatLng> vertices) {
        return BigDecimal.valueOf(calculateCenter(vertices)[0]);
    }

    public static BigDecimal calculateCenterLat(List<FlightRouteService.LatLng> vertices) {
        return BigDecimal.valueOf(calculateCenter(vertices)[1]);
    }
}
