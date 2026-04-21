package com.ramennoodles.delivery.integration;

import com.ramennoodles.delivery.enums.ZoneType;
import com.ramennoodles.delivery.model.Coordinate;
import com.ramennoodles.delivery.model.GeofenceZone;
import com.ramennoodles.delivery.model.Rider;
import com.ramennoodles.delivery.model.RoutePlan;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CenterDiv transport adapter.
 *
 * This implementation is intentionally resilient:
 * - If CenterDiv classes are on classpath, it invokes their facade/service via reflection.
 * - If they are unavailable, it falls back to deterministic local behavior so integration paths
 *   remain executable during development and demos.
 */
public class CenterDivTransportAdapter implements ITransportLogisticsService {

    private final List<Object> partnerTargets;
    private final Map<String, Rider> riderCache;

    public CenterDivTransportAdapter() {
        this.partnerTargets = new ArrayList<>();
        this.riderCache = new HashMap<>();

        try {
            Class<?> facadeClass = Class.forName("facade.TransportFacade");
            Object facade = facadeClass.getDeclaredConstructor().newInstance();
            partnerTargets.add(facade);

            Object service = tryInvokeOnTarget(facade, "getTransportService");
            if (service != null) {
                partnerTargets.add(0, service);
            }

            System.out.println("[CenterDivAdapter] Connected to CenterDiv TransportFacade.");
        } catch (Exception ex) {
            System.out.println("[CenterDivAdapter] CenterDiv classes not found, using local fallback mode.");
        }
    }

    @Override
    public Rider getRiderDetails(String riderId) {
        if (riderId == null || riderId.isBlank()) {
            return null;
        }

        Rider cached = riderCache.get(riderId);
        if (cached != null) {
            return cached;
        }

        for (Rider rider : getAvailableRiders("ALL")) {
            if (riderId.equals(rider.getRiderId())) {
                return rider;
            }
        }

        Rider fallback = Rider.createProfile("CenterDiv Rider " + riderId, "N/A", "Bike");
        fallback.setRiderId(riderId);
        fallback.activate();
        riderCache.put(riderId, fallback);
        return fallback;
    }

    @Override
    public List<Rider> getAvailableRiders(String zone) {
        List<Rider> riders = new ArrayList<>();

        Object carriersObj = tryInvoke("getAllCarriers", "Road");
        if (carriersObj instanceof List<?> carrierList) {
            for (Object carrier : carrierList) {
                Rider rider = mapCarrierToRider(carrier);
                if (rider != null && rider.isAvailable()) {
                    riders.add(rider);
                }
            }
        }

        if (!riders.isEmpty()) {
            return riders;
        }

        return createFallbackRiders(zone);
    }

    @Override
    public RoutePlan calculateOptimalRoute(String orderId, Coordinate pickup,
                                           Coordinate dropoff, List<Coordinate> waypoints) {
        List<Coordinate> points = new ArrayList<>();
        if (pickup != null) {
            points.add(pickup);
        }
        if (waypoints != null) {
            for (Coordinate waypoint : waypoints) {
                if (waypoint != null) {
                    points.add(waypoint);
                }
            }
        }
        if (dropoff != null) {
            points.add(dropoff);
        }

        if (points.size() < 2 && pickup != null && dropoff != null) {
            points.clear();
            points.add(pickup);
            points.add(dropoff);
        }

        if (points.size() < 2) {
            points.clear();
            points.add(new Coordinate(12.9352, 77.6245));
            points.add(new Coordinate(12.9716, 77.5946));
        }

        Object routeResult = tryInvoke(
                "optimizeRoute",
                formatCoordinate(pickup),
                formatCoordinate(dropoff),
                "delivery"
        );

        if (routeResult instanceof Map<?, ?> routeMap) {
            List<Coordinate> mapped = extractCoordinates(routeMap.get("waypoints"));
            if (mapped.size() >= 2) {
                points = mapped;
            }
        }

        String safeOrderId = (orderId == null || orderId.isBlank()) ? "ORD-EXT" : orderId;
        RoutePlan route = RoutePlan.create(safeOrderId, points);
        route.optimizeRoute();
        return route;
    }

    @Override
    public List<GeofenceZone> getLogisticsHubZones() {
        List<GeofenceZone> zones = new ArrayList<>();

        Object territoriesObj = tryInvoke("getAllTerritories");
        if (territoriesObj instanceof List<?> territoryList) {
            for (Object territory : territoryList) {
                String territoryId = readString(territory, "getTerritoryId", "HUB-" + UUID.randomUUID());
                String zoneName = readString(territory, "getZoneName", territoryId);
                Coordinate center = pseudoCoordinate(zoneName);

                GeofenceZone zone = GeofenceZone.create("CENTERDIV-HUB", center.getLatitude(), center.getLongitude(),
                        500, ZoneType.WAREHOUSE);
                zone.setZoneId(territoryId);
                zones.add(zone);
            }
        }

        if (!zones.isEmpty()) {
            return zones;
        }

        GeofenceZone fallback = GeofenceZone.create("CENTERDIV-HUB", 12.9716, 77.5946, 500, ZoneType.WAREHOUSE);
        fallback.setZoneId("HUB-BLR-001");
        zones.add(fallback);
        return zones;
    }

    @Override
    public void reportVehicleHealth(String riderId, String healthReport) {
        // Best-effort bridge: partner method may not exist in all versions.
        Object ignored = tryInvoke("reportVehicleHealth", riderId, healthReport);
        if (ignored == null) {
            System.out.println("[CenterDivAdapter] Vehicle health report (local only): rider="
                    + riderId + ", report=" + healthReport);
        }
    }

    @Override
    public void notifyRiderAvailable(String riderId) {
        Rider rider = riderCache.get(riderId);
        if (rider != null) {
            rider.activate();
        }

        Object ignored = tryInvoke("notifyRiderAvailable", riderId);
        if (ignored == null) {
            System.out.println("[CenterDivAdapter] Rider availability notified (local only): " + riderId);
        }
    }

    @Override
    public String getVehicleType(String riderId) {
        Rider rider = getRiderDetails(riderId);
        return rider != null ? rider.getVehicleType() : "Unknown";
    }

    private Rider mapCarrierToRider(Object carrier) {
        if (carrier == null) {
            return null;
        }

        String carrierId = readString(carrier, "getCarrierId", null);
        if (carrierId == null || carrierId.isBlank()) {
            return null;
        }

        String name = readString(carrier, "getCarrierName", "CenterDiv Carrier " + carrierId);
        String transportMode = readString(carrier, "getTransportMode", "Road");
        boolean available = readBoolean(carrier, "isAvailable", true);

        Rider rider = Rider.createProfile(name, "N/A", transportMode);
        rider.setRiderId(carrierId);
        if (available) {
            rider.activate();
        }

        riderCache.put(rider.getRiderId(), rider);
        return rider;
    }

    private List<Rider> createFallbackRiders(String zone) {
        List<Rider> fallback = new ArrayList<>();
        String safeZone = sanitizeZone(zone);

        for (int i = 1; i <= 3; i++) {
            String riderId = "CTR-" + safeZone + "-" + String.format("%02d", i);
            Rider rider = riderCache.get(riderId);
            if (rider == null) {
                rider = Rider.createProfile("CenterDiv Rider " + i, "N/A", i % 2 == 0 ? "Van" : "Bike");
                rider.setRiderId(riderId);
                rider.activate();
                riderCache.put(riderId, rider);
            }
            if (rider.isAvailable()) {
                fallback.add(rider);
            }
        }

        return fallback;
    }

    private String sanitizeZone(String zone) {
        if (zone == null || zone.isBlank()) {
            return "GEN";
        }
        String safe = zone.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return safe.isBlank() ? "GEN" : safe;
    }

    private Coordinate pseudoCoordinate(String seed) {
        int hash = Math.abs(seed == null ? 0 : seed.hashCode());
        double lat = 12.80 + (hash % 1000) / 10000.0;
        double lng = 77.40 + ((hash / 1000) % 1000) / 10000.0;
        return new Coordinate(lat, lng);
    }

    private List<Coordinate> extractCoordinates(Object value) {
        List<Coordinate> coordinates = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return coordinates;
        }

        for (Object item : list) {
            if (item instanceof Coordinate coordinate) {
                coordinates.add(coordinate);
                continue;
            }

            if (item instanceof Map<?, ?> map) {
                Double lat = asDouble(map.get("latitude"));
                Double lng = asDouble(map.get("longitude"));
                if (lat != null && lng != null) {
                    coordinates.add(new Coordinate(lat, lng));
                }
                continue;
            }

            if (item instanceof double[] arr && arr.length >= 2) {
                coordinates.add(new Coordinate(arr[0], arr[1]));
            }
        }

        return coordinates;
    }

    private String formatCoordinate(Coordinate coordinate) {
        if (coordinate == null) {
            return "";
        }
        return coordinate.getLatitude() + "," + coordinate.getLongitude();
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String readString(Object target, String getter, String fallback) {
        try {
            Object value = invokeMethod(target, getter);
            if (value == null) {
                return fallback;
            }
            String text = String.valueOf(value);
            return text.isBlank() ? fallback : text;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean readBoolean(Object target, String getter, boolean fallback) {
        try {
            Object value = invokeMethod(target, getter);
            if (value instanceof Boolean flag) {
                return flag;
            }
            return fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Object tryInvoke(String methodName, Object... args) {
        for (Object target : partnerTargets) {
            Object value = tryInvokeOnTarget(target, methodName, args);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object tryInvokeOnTarget(Object target, String methodName, Object... args) {
        try {
            return invokeMethod(target, methodName, args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeMethod(Object target, String methodName, Object... args) throws Exception {
        Method method = findCompatibleMethod(target.getClass(), methodName, args);
        if (method == null) {
            throw new NoSuchMethodException(methodName + " not found on " + target.getClass().getName());
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Method findCompatibleMethod(Class<?> type, String methodName, Object[] args) {
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != args.length) {
                continue;
            }

            boolean compatible = true;
            for (int i = 0; i < paramTypes.length; i++) {
                Object arg = args[i];
                if (!isCompatible(paramTypes[i], arg)) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private boolean isCompatible(Class<?> paramType, Object arg) {
        if (arg == null) {
            return !paramType.isPrimitive();
        }

        Class<?> argType = arg.getClass();
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }

        if (!paramType.isPrimitive()) {
            return false;
        }

        return (paramType == boolean.class && argType == Boolean.class)
                || (paramType == int.class && argType == Integer.class)
                || (paramType == long.class && argType == Long.class)
                || (paramType == double.class && argType == Double.class)
                || (paramType == float.class && argType == Float.class)
                || (paramType == short.class && argType == Short.class)
                || (paramType == byte.class && argType == Byte.class)
                || (paramType == char.class && argType == Character.class);
    }
}
