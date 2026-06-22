package com.agri.claim.ai.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightRouteService {

    private static final double DEFAULT_FLIGHT_HEIGHT = 100.0;
    private static final double DEFAULT_FRONT_OVERLAP = 80.0;
    private static final double DEFAULT_SIDE_OVERLAP = 60.0;
    private static final double DEFAULT_CAMERA_FOCAL_LENGTH = 8.0;
    private static final double DEFAULT_SENSOR_WIDTH = 17.3;
    private static final double DEFAULT_SENSOR_HEIGHT = 13.0;
    private static final double DEFAULT_IMAGE_WIDTH = 5472.0;
    private static final double DEFAULT_IMAGE_HEIGHT = 3648.0;
    private static final double EARTH_RADIUS = 6378137.0;

    public RoutePlan generateZigzagRoute(RoutePlanRequest request) {
        log.info("生成之字形航线 | 顶点数: {} | 飞行高度: {}m | 航向重叠: {}% | 旁向重叠: {}%",
                request.getPolygonVertices().size(), request.getFlightHeight(),
                request.getFrontOverlap(), request.getSideOverlap());

        List<LatLng> polygon = request.getPolygonVertices();
        validatePolygon(polygon);

        double flightHeight = request.getFlightHeight() > 0 ? request.getFlightHeight() : DEFAULT_FLIGHT_HEIGHT;
        double frontOverlap = request.getFrontOverlap() > 0 ? request.getFrontOverlap() : DEFAULT_FRONT_OVERLAP;
        double sideOverlap = request.getSideOverlap() > 0 ? request.getSideOverlap() : DEFAULT_SIDE_OVERLAP;

        double[] gsdAndSize = calculateGroundSamplingDistance(flightHeight);
        double gsd = gsdAndSize[0];
        double groundWidth = gsdAndSize[1];
        double groundHeight = gsdAndSize[2];

        double spacingAlong = groundHeight * (100 - frontOverlap) / 100;
        double spacingAcross = groundWidth * (100 - sideOverlap) / 100;

        double[] bbox = calculateBoundingBox(polygon);
        double minLon = bbox[0], minLat = bbox[1], maxLon = bbox[2], maxLat = bbox[3];

        double angle = findOptimalFlightAngle(polygon, bbox);
        log.info("最优飞行方向: {}°", Math.toDegrees(angle));

        List<LatLng> rotatedPolygon = rotatePolygon(polygon, bbox, -angle);
        double[] rotatedBbox = calculateBoundingBox(rotatedPolygon);

        double flightWidth = rotatedBbox[2] - rotatedBbox[0];
        double flightHeightRange = rotatedBbox[3] - rotatedBbox[1];
        int numLines = (int) Math.ceil(flightWidth / spacingAcross) + 1;

        List<Waypoint> waypoints = new ArrayList<>();
        int wpIndex = 0;

        double[] center = new double[]{(bbox[0] + bbox[2]) / 2, (bbox[1] + bbox[3]) / 2};

        for (int i = 0; i < numLines; i++) {
            double x = rotatedBbox[0] + i * spacingAcross;
            boolean forward = i % 2 == 0;

            LatLng startRotated = new LatLng(rotatedBbox[1], x);
            LatLng endRotated = new LatLng(rotatedBbox[3], x);

            LatLng start = rotatePointBack(startRotated, center, angle);
            LatLng end = rotatePointBack(endRotated, center, angle);

            double yStep = forward ? spacingAlong : -spacingAlong;
            double currentY = forward ? rotatedBbox[1] : rotatedBbox[3];

            while ((forward && currentY <= rotatedBbox[3]) || (!forward && currentY >= rotatedBbox[1])) {
                LatLng wpRotated = new LatLng(currentY, x);
                LatLng wpCoord = rotatePointBack(wpRotated, center, angle);

                if (isPointInPolygon(wpCoord, polygon)) {
                    Waypoint wp = new Waypoint();
                    wp.setIndex(wpIndex++);
                    wp.setLatitude(BigDecimal.valueOf(wpCoord.lat).setScale(8, RoundingMode.HALF_UP));
                    wp.setLongitude(BigDecimal.valueOf(wpCoord.lon).setScale(8, RoundingMode.HALF_UP));
                    wp.setAltitude(BigDecimal.valueOf(flightHeight).setScale(2, RoundingMode.HALF_UP));
                    wp.setSpeed(BigDecimal.valueOf(request.getFlightSpeed() > 0 ? request.getFlightSpeed() : 5.0)
                            .setScale(2, RoundingMode.HALF_UP));
                    wp.setYawAngle(BigDecimal.ZERO);
                    wp.setActions(Collections.singletonList("TAKE_PHOTO"));
                    waypoints.add(wp);
                }
                currentY += yStep;
            }
        }

        if (request.getObstacles() != null && !request.getObstacles().isEmpty()) {
            waypoints = applyObstacleAvoidance(waypoints, request.getObstacles(), flightHeight, polygon);
        }

        Waypoint takeoff = new Waypoint();
        takeoff.setIndex(-1);
        takeoff.setLatitude(waypoints.get(0).getLatitude());
        takeoff.setLongitude(waypoints.get(0).getLongitude());
        takeoff.setAltitude(BigDecimal.ZERO);
        takeoff.setActions(Collections.singletonList("TAKEOFF"));

        Waypoint land = new Waypoint();
        land.setIndex(-2);
        land.setLatitude(waypoints.get(waypoints.size() - 1).getLatitude());
        land.setLongitude(waypoints.get(waypoints.size() - 1).getLongitude());
        land.setAltitude(BigDecimal.ZERO);
        land.setActions(Collections.singletonList("LAND"));

        List<Waypoint> fullRoute = new ArrayList<>();
        fullRoute.add(takeoff);
        fullRoute.addAll(waypoints);
        fullRoute.add(land);

        double totalDistance = calculateTotalDistance(waypoints);
        double estimatedTime = totalDistance / (request.getFlightSpeed() > 0 ? request.getFlightSpeed() : 5.0) / 60;
        int photoCount = (int) waypoints.stream().filter(w -> w.getActions().contains("TAKE_PHOTO")).count();
        double estimatedArea = calculateCoveredArea(polygon);

        RoutePlan plan = new RoutePlan();
        plan.setFlightHeight(BigDecimal.valueOf(flightHeight));
        plan.setFrontOverlap(BigDecimal.valueOf(frontOverlap));
        plan.setSideOverlap(BigDecimal.valueOf(sideOverlap));
        plan.setGsd(BigDecimal.valueOf(gsd).setScale(3, RoundingMode.HALF_UP));
        plan.setGroundCoverWidth(BigDecimal.valueOf(groundWidth).setScale(3, RoundingMode.HALF_UP));
        plan.setGroundCoverHeight(BigDecimal.valueOf(groundHeight).setScale(3, RoundingMode.HALF_UP));
        plan.setSpacingAlong(BigDecimal.valueOf(spacingAlong).setScale(3, RoundingMode.HALF_UP));
        plan.setSpacingAcross(BigDecimal.valueOf(spacingAcross).setScale(3, RoundingMode.HALF_UP));
        plan.setFlightAngle(BigDecimal.valueOf(Math.toDegrees(angle)).setScale(2, RoundingMode.HALF_UP));
        plan.setWaypoints(fullRoute);
        plan.setWaypointCount(fullRoute.size());
        plan.setEstimatedTime(BigDecimal.valueOf(estimatedTime).setScale(2, RoundingMode.HALF_UP));
        plan.setEstimatedDistance(BigDecimal.valueOf(totalDistance).setScale(3, RoundingMode.HALF_UP));
        plan.setEstimatedArea(BigDecimal.valueOf(estimatedArea).setScale(4, RoundingMode.HALF_UP));
        plan.setPhotoCount(photoCount);
        plan.setEstimatedBattery(calculateEstimatedBattery(estimatedTime, fullRoute.size()));

        log.info("航线生成完成 | 航点数: {} | 航程: {}m | 预计时间: {}min | 预计照片: {}张",
                plan.getWaypointCount(), plan.getEstimatedDistance(),
                plan.getEstimatedTime(), plan.getPhotoCount());

        return plan;
    }

    public double[] calculateGroundSamplingDistance(double flightHeight) {
        double gsd = (flightHeight * DEFAULT_SENSOR_WIDTH * 1000)
                / (DEFAULT_CAMERA_FOCAL_LENGTH * DEFAULT_IMAGE_WIDTH);
        double groundWidth = gsd * DEFAULT_IMAGE_WIDTH / 1000;
        double groundHeight = gsd * DEFAULT_IMAGE_HEIGHT / 1000;
        return new double[]{gsd, groundWidth, groundHeight};
    }

    private double findOptimalFlightAngle(List<LatLng> polygon, double[] bbox) {
        double centerLon = (bbox[0] + bbox[2]) / 2;
        double centerLat = (bbox[1] + bbox[3]) / 2;

        double bestAngle = 0;
        double minNumLines = Double.MAX_VALUE;

        for (int deg = 0; deg < 90; deg += 5) {
            double angle = Math.toRadians(deg);
            double[] c = new double[]{centerLon, centerLat};
            List<LatLng> rotated = rotatePolygon(polygon, bbox, -angle);
            double[] rb = calculateBoundingBox(rotated);

            double width = rb[2] - rb[0];
            double[] gsdSize = calculateGroundSamplingDistance(100);
            double spacing = gsdSize[1] * (100 - 60) / 100;
            double numLines = width / spacing;

            if (numLines < minNumLines) {
                minNumLines = numLines;
                bestAngle = angle;
            }
        }
        return bestAngle;
    }

    public List<Waypoint> applyObstacleAvoidance(List<Waypoint> waypoints,
                                                  List<Obstacle> obstacles,
                                                  double defaultHeight,
                                                  List<LatLng> polygon) {
        List<Waypoint> adjusted = new ArrayList<>();
        for (Waypoint wp : waypoints) {
            BigDecimal newAlt = wp.getAltitude();
            for (Obstacle obs : obstacles) {
                double dist = haversineDistance(
                        wp.getLatitude().doubleValue(), wp.getLongitude().doubleValue(),
                        obs.getLatitude(), obs.getLongitude());

                if (dist < obs.getRadius() * 1.5) {
                    double safeHeight = obs.getHeight() + 20;
                    if (safeHeight > newAlt.doubleValue()) {
                        newAlt = BigDecimal.valueOf(safeHeight).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
            Waypoint newWp = new Waypoint();
            newWp.setIndex(wp.getIndex());
            newWp.setLatitude(wp.getLatitude());
            newWp.setLongitude(wp.getLongitude());
            newWp.setAltitude(newAlt);
            newWp.setSpeed(wp.getSpeed());
            newWp.setYawAngle(wp.getYawAngle());
            newWp.setActions(wp.getActions());
            adjusted.add(newWp);
        }

        List<Waypoint> withDetour = new ArrayList<>();
        for (int i = 0; i < adjusted.size(); i++) {
            withDetour.add(adjusted.get(i));

            if (i < adjusted.size() - 1) {
                Waypoint curr = adjusted.get(i);
                Waypoint next = adjusted.get(i + 1);

                for (Obstacle obs : obstacles) {
                    double distCurr = haversineDistance(
                            curr.getLatitude().doubleValue(), curr.getLongitude().doubleValue(),
                            obs.getLatitude(), obs.getLongitude());
                    double distNext = haversineDistance(
                            next.getLatitude().doubleValue(), next.getLongitude().doubleValue(),
                            obs.getLatitude(), obs.getLongitude());

                    if (distCurr < obs.getRadius() * 2 || distNext < obs.getRadius() * 2) {
                        double bearing = Math.atan2(
                                next.getLongitude().doubleValue() - curr.getLongitude().doubleValue(),
                                next.getLatitude().doubleValue() - curr.getLatitude().doubleValue());
                        double perpBearing = bearing + Math.PI / 2;
                        double detourDist = obs.getRadius() * 3;

                        Waypoint detour = new Waypoint();
                        detour.setIndex(-3);
                        detour.setLatitude(BigDecimal.valueOf(curr.getLatitude().doubleValue()
                                + detourDist * Math.cos(perpBearing) / (EARTH_RADIUS * Math.PI / 180))
                                .setScale(8, RoundingMode.HALF_UP));
                        detour.setLongitude(BigDecimal.valueOf(curr.getLongitude().doubleValue()
                                + detourDist * Math.sin(perpBearing) / (EARTH_RADIUS * Math.PI / 180
                                * Math.cos(Math.toRadians(curr.getLatitude().doubleValue()))))
                                .setScale(8, RoundingMode.HALF_UP));
                        detour.setAltitude(BigDecimal.valueOf(obs.getHeight() + 30)
                                .setScale(2, RoundingMode.HALF_UP));
                        detour.setSpeed(BigDecimal.valueOf(3.0));
                        detour.setActions(Collections.emptyList());
                        withDetour.add(detour);
                        log.info("添加避障绕行航点 | 障碍: {} | 绕行高度: {}m", obs.getName(), detour.getAltitude());
                    }
                }
            }
        }
        return withDetour;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private List<LatLng> rotatePolygon(List<LatLng> polygon, double[] bbox, double angle) {
        double[] center = new double[]{(bbox[0] + bbox[2]) / 2, (bbox[1] + bbox[3]) / 2};
        List<LatLng> rotated = new ArrayList<>();
        for (LatLng p : polygon) {
            rotated.add(rotatePoint(p, center, angle));
        }
        return rotated;
    }

    private LatLng rotatePoint(LatLng p, double[] center, double angle) {
        double dx = p.lon - center[0];
        double dy = p.lat - center[1];
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rx = dx * cos - dy * sin;
        double ry = dx * sin + dy * cos;
        return new LatLng(center[1] + ry, center[0] + rx);
    }

    private LatLng rotatePointBack(LatLng p, double[] center, double angle) {
        return rotatePoint(p, center, angle);
    }

    private double[] calculateBoundingBox(List<LatLng> polygon) {
        double minLon = Double.MAX_VALUE, minLat = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        for (LatLng p : polygon) {
            minLon = Math.min(minLon, p.lon);
            minLat = Math.min(minLat, p.lat);
            maxLon = Math.max(maxLon, p.lon);
            maxLat = Math.max(maxLat, p.lat);
        }
        return new double[]{minLon, minLat, maxLon, maxLat};
    }

    private boolean isPointInPolygon(LatLng pt, List<LatLng> poly) {
        boolean inside = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            LatLng pi = poly.get(i), pj = poly.get(j);
            if (((pi.lat > pt.lat) != (pj.lat > pt.lat))
                    && (pt.lon < (pj.lon - pi.lon) * (pt.lat - pi.lat) / (pj.lat - pi.lat) + pi.lon)) {
                inside = !inside;
            }
        }
        return inside;
    }

    private void validatePolygon(List<LatLng> polygon) {
        if (polygon == null || polygon.size() < 3) {
            throw new IllegalArgumentException("多边形至少需要3个顶点");
        }
        if (!polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
            polygon.add(new LatLng(polygon.get(0)));
        }
    }

    private double calculateTotalDistance(List<Waypoint> waypoints) {
        double dist = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Waypoint a = waypoints.get(i), b = waypoints.get(i + 1);
            dist += haversineDistance(
                    a.getLatitude().doubleValue(), a.getLongitude().doubleValue(),
                    b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
        }
        return dist;
    }

    private double calculateCoveredArea(List<LatLng> polygon) {
        double area = 0;
        for (int i = 0; i < polygon.size() - 1; i++) {
            LatLng p1 = polygon.get(i), p2 = polygon.get(i + 1);
            area += Math.toRadians(p2.lon - p1.lon)
                    * (2 + Math.sin(Math.toRadians(p1.lat)) + Math.sin(Math.toRadians(p2.lat)));
        }
        return Math.abs(area * EARTH_RADIUS * EARTH_RADIUS / 2) / 666.667;
    }

    private BigDecimal calculateEstimatedBattery(double minutes, int waypointCount) {
        double battery = Math.min(100, 25 + minutes * 0.8 + waypointCount * 0.1);
        return BigDecimal.valueOf(Math.max(10, 100 - battery)).setScale(0, RoundingMode.HALF_UP);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatLng {
        double lat;
        double lon;
        public LatLng(LatLng other) { this.lat = other.lat; this.lon = other.lon; }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LatLng)) return false;
            LatLng other = (LatLng) o;
            return Math.abs(lat - other.lat) < 1e-9 && Math.abs(lon - other.lon) < 1e-9;
        }
    }

    @Data
    public static class RoutePlanRequest {
        private List<LatLng> polygonVertices;
        private double flightHeight;
        private double frontOverlap;
        private double sideOverlap;
        private double flightSpeed;
        private List<Obstacle> obstacles;
        private Long userId;
        private Long missionId;
    }

    @Data
    public static class RoutePlan {
        private BigDecimal flightHeight;
        private BigDecimal frontOverlap;
        private BigDecimal sideOverlap;
        private BigDecimal gsd;
        private BigDecimal groundCoverWidth;
        private BigDecimal groundCoverHeight;
        private BigDecimal spacingAlong;
        private BigDecimal spacingAcross;
        private BigDecimal flightAngle;
        private List<Waypoint> waypoints;
        private int waypointCount;
        private BigDecimal estimatedTime;
        private BigDecimal estimatedDistance;
        private BigDecimal estimatedArea;
        private int photoCount;
        private BigDecimal estimatedBattery;
    }

    @Data
    public static class Waypoint {
        private int index;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal altitude;
        private BigDecimal speed;
        private BigDecimal yawAngle;
        private BigDecimal gimbalAngle = BigDecimal.valueOf(-90);
        private List<String> actions;
        private BigDecimal stayTime = BigDecimal.ZERO;
    }

    @Data
    public static class Obstacle {
        private String name;
        private String type;
        private double latitude;
        private double longitude;
        private double radius;
        private double height;
    }

    @Data
    public static class CameraParam {
        private double focalLength = DEFAULT_CAMERA_FOCAL_LENGTH;
        private double sensorWidth = DEFAULT_SENSOR_WIDTH;
        private double sensorHeight = DEFAULT_SENSOR_HEIGHT;
        private double imageWidth = DEFAULT_IMAGE_WIDTH;
        private double imageHeight = DEFAULT_IMAGE_HEIGHT;
    }
}
