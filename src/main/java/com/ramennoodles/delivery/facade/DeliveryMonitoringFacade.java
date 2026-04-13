package com.ramennoodles.delivery.facade;

import com.ramennoodles.delivery.enums.*;
import com.ramennoodles.delivery.model.*;
import com.ramennoodles.delivery.observer.*;
import com.ramennoodles.delivery.service.*;
import com.ramennoodles.delivery.strategy.*;

import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 * DELIVERY MONITORING FACADE
 * Design Pattern: Facade
 * ═══════════════════════════════════════════════════════════════
 * 
 * Provides a single, simplified entry point for all delivery 
 * monitoring operations. External subsystems (VERTEX, DEI Hires,
 * CenterDiv) interact with our system through this facade.
 * 
 * Internally coordinates all services:
 * - GPSTrackingService
 * - GeofencingService
 * - ETAService
 * - StatusUpdateService
 * - PODService
 * - NotificationService
 * - RoutePlanService
 * - FleetDashboardService
 */
public class DeliveryMonitoringFacade {

    // Core services
    private final GPSTrackingService gpsService;
    private final GeofencingService geofencingService;
    private final ETAService etaService;
    private final StatusUpdateService statusService;
    private final PODService podService;
    private final NotificationService notificationService;
    private final RoutePlanService routePlanService;
    private final FleetDashboardService dashboardService;
    private final DeliveryEventManager eventManager;

    // In-memory order & customer registry
    private final Map<String, Order> orders;
    private final Map<String, Customer> customers;
    private final Map<String, Rider> riders;

    /**
     * Constructor: initializes all services with a shared event manager.
     */
    public DeliveryMonitoringFacade() {
        this.eventManager = new DeliveryEventManager();
        this.gpsService = new GPSTrackingService(eventManager);
        this.geofencingService = new GeofencingService(eventManager);
        this.etaService = new ETAService(eventManager);
        this.statusService = new StatusUpdateService(eventManager);
        this.podService = new PODService(eventManager);
        this.notificationService = new NotificationService();
        this.routePlanService = new RoutePlanService();
        this.dashboardService = new FleetDashboardService(gpsService, statusService, etaService);

        this.orders = new HashMap<>();
        this.customers = new HashMap<>();
        this.riders = new HashMap<>();
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 1: Setup & Registration
    // ═══════════════════════════════════════════════════════════

    /**
     * Registers a new customer in the system.
     */
    public Customer registerCustomer(String name, String email, String phone) {
        Customer customer = Customer.createProfile(name, email, phone);
        customers.put(customer.getCustomerId(), customer);
        System.out.println("✅ Customer registered: " + customer);
        return customer;
    }

    /**
     * Registers a new rider in the system.
     */
    public Rider registerRider(String name, String phone, String vehicleType) {
        Rider rider = Rider.createProfile(name, phone, vehicleType);
        riders.put(rider.getRiderId(), rider);
        dashboardService.trackRider(rider);
        System.out.println("✅ Rider registered: " + rider);
        return rider;
    }

    /**
     * Registers a GPS device for a rider and activates the rider.
     */
    public Device registerDeviceForRider(Rider rider) {
        Device device = gpsService.registerDevice(rider.getRiderId());
        rider.activate();
        System.out.println("✅ Device registered: " + device);
        return device;
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 2: Order Creation & Tracking Initialization
    // ═══════════════════════════════════════════════════════════

    /**
     * Creates a new delivery order and initializes tracking.
     * This is the main entry point called by VERTEX/DEI Hires.
     */
    public Order createAndInitializeDelivery(String customerId, String pickupAddress,
                                              String dropoffAddress, Coordinate pickupCoord,
                                              Coordinate dropoffCoord) {
        // Create order
        Order order = Order.create(customerId, pickupAddress, dropoffAddress,
                pickupCoord, dropoffCoord);
        orders.put(order.getOrderId(), order);

        // Initialize status tracking
        statusService.initializeOrder(order.getOrderId());

        // Register destination for ETA
        etaService.registerOrderDestination(order.getOrderId(), dropoffCoord);

        // Create geofence zones (200m radius)
        geofencingService.createZonesForOrder(order, 200);

        // Create route plan
        routePlanService.createRoutePlan(order.getOrderId(), pickupCoord, dropoffCoord);

        // Track in dashboard
        dashboardService.trackOrder(order);

        // Publish event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("orderId", order.getOrderId());
        eventData.put("customerId", customerId);
        eventManager.publish(DeliveryEventType.ORDER_CREATED, eventData);

        // Notify customer
        notificationService.notifyMilestone(order.getOrderId(), customerId,
                "Your order has been placed! We'll notify you when a rider is assigned.");

        System.out.println("✅ Order created: " + order);
        return order;
    }

    /**
     * Assigns a rider to an order and starts active tracking.
     */
    public void assignRiderToOrder(String orderId, String riderId) {
        Order order = orders.get(orderId);
        Rider rider = riders.get(riderId);

        if (order == null) throw new IllegalArgumentException("Unknown order: " + orderId);
        if (rider == null) throw new IllegalArgumentException("Unknown rider: " + riderId);

        // Assign rider
        order.assignRider(riderId);
        routePlanService.assignRider(orderId, riderId);
        rider.updateStatus(RiderStatus.ON_DELIVERY);

        // Update status
        statusService.updateStatus(orderId, OrderStatus.ASSIGNED, "SYSTEM", "system");

        // Publish event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("orderId", orderId);
        eventData.put("riderId", riderId);
        eventData.put("riderName", rider.getName());
        eventManager.publish(DeliveryEventType.RIDER_ASSIGNED, eventData);

        // Notify customer
        notificationService.notifyMilestone(orderId, order.getCustomerId(),
                "Rider " + rider.getName() + " has been assigned to your delivery!");

        System.out.println("✅ Rider " + rider.getName() + " assigned to order " + orderId);
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 3: Live Tracking (GPS + Geofencing + ETA)
    // ═══════════════════════════════════════════════════════════

    /**
     * Processes a GPS ping and performs all related checks.
     * This is called repeatedly during active delivery.
     */
    public GPSPing processLocationUpdate(String deviceId, String orderId,
                                          double latitude, double longitude) {
        // Process GPS ping
        GPSPing ping = gpsService.processGPSPing(deviceId, latitude, longitude);
        String riderId = ping.getRiderId();

        // Check geofences
        List<GeofenceEvent> events = geofencingService.checkGeofences(orderId, riderId,
                latitude, longitude);

        // Handle geofence events
        for (GeofenceEvent event : events) {
            handleGeofenceEvent(orderId, event);
        }

        // Calculate new ETA
        etaService.calculateETA(orderId, latitude, longitude);

        return ping;
    }

    /**
     * Handles a geofence event by updating status and notifying customer.
     */
    private void handleGeofenceEvent(String orderId, GeofenceEvent event) {
        Order order = orders.get(orderId);
        if (order == null) return;

        // Determine the zone type from the event
        List<GeofenceZone> zones = geofencingService.getZonesForOrder(orderId);
        for (GeofenceZone zone : zones) {
            if (zone.getZoneId().equals(event.getZoneId())) {
                if (event.getEventType() == EventType.ENTRY) {
                    if (zone.getZoneType() == ZoneType.PICKUP) {
                        System.out.println("📍 Rider arrived at PICKUP zone");
                    } else if (zone.getZoneType() == ZoneType.DROPOFF) {
                        System.out.println("📍 Rider arrived at DROPOFF zone — nearing destination!");
                        // Auto-transition to ARRIVING
                        OrderStatus current = statusService.getCurrentStatus(orderId);
                        if (current == OrderStatus.IN_TRANSIT) {
                            statusService.updateStatus(orderId, OrderStatus.ARRIVING,
                                    "GEOFENCE", "system");
                            notificationService.notifyMilestone(orderId, order.getCustomerId(),
                                    "Your rider is arriving! Please be ready to receive your order.");
                        }
                    }
                }
            }
        }
    }

    /**
     * Manually updates order status (used for pickup, in-transit transitions).
     */
    public void updateOrderStatus(String orderId, OrderStatus newStatus, String changedBy) {
        Order order = orders.get(orderId);
        if (order == null) throw new IllegalArgumentException("Unknown order: " + orderId);

        statusService.updateStatus(orderId, newStatus, "MANUAL", changedBy);
        order.updateStatus(newStatus);

        // Send milestone notifications
        String message;
        switch (newStatus) {
            case PICKED_UP:
                message = "Your order has been picked up and is on its way!";
                break;
            case IN_TRANSIT:
                message = "Your order is in transit. Track it live!";
                break;
            case ARRIVING:
                message = "Your rider is arriving! Please be ready.";
                break;
            case DELIVERED:
                message = "Your order has been delivered! Thank you.";
                break;
            case FAILED:
                message = "Delivery attempt failed. We'll try again soon.";
                break;
            default:
                message = "Order status updated to: " + newStatus.getDescription();
        }
        notificationService.notifyMilestone(orderId, order.getCustomerId(), message);
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 4: Delivery Completion (POD)
    // ═══════════════════════════════════════════════════════════

    /**
     * Completes a delivery by submitting proof of delivery.
     */
    public PODRecord completeDelivery(String orderId, String signatureFile,
                                       String photoFile, String notes) {
        Order order = orders.get(orderId);
        if (order == null) throw new IllegalArgumentException("Unknown order: " + orderId);

        String riderId = order.getRiderId();

        // Submit POD
        PODRecord pod = podService.submitPOD(orderId, signatureFile, photoFile, notes, riderId);

        // Update status to DELIVERED
        statusService.updateStatus(orderId, OrderStatus.DELIVERED, "POD", riderId);
        order.updateStatus(OrderStatus.DELIVERED);

        // Release rider
        Rider rider = riders.get(riderId);
        if (rider != null) {
            rider.activate(); // Back to available
        }

        // Notify customer
        notificationService.notifyMilestone(orderId, order.getCustomerId(),
                "Your order has been delivered! Thank you for choosing our service. 🎉");

        System.out.println("✅ Delivery completed for order " + orderId);
        return pod;
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 5: Query Methods (for other subsystems)
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the current status of an order.
     */
    public OrderStatus getOrderStatus(String orderId) {
        return statusService.getCurrentStatus(orderId);
    }

    /**
     * Gets the latest ETA for an order.
     */
    public ETARecord getLatestETA(String orderId) {
        return etaService.getLatestETA(orderId);
    }

    /**
     * Gets the latest GPS position of a rider.
     */
    public GPSPing getRiderPosition(String riderId) {
        return gpsService.getLatestPing(riderId);
    }

    /**
     * Gets the POD for an order.
     */
    public PODRecord getPOD(String orderId) {
        return podService.getPODByOrder(orderId);
    }

    /**
     * Gets the full status history (audit trail) for an order.
     */
    public List<DeliveryStatusLog> getStatusHistory(String orderId) {
        return statusService.getStatusHistory(orderId);
    }

    /**
     * Generates a tracking URL for customer self-service.
     */
    public String getTrackingURL(String orderId) {
        return "https://track.ramennoodles.delivery/" + orderId;
    }

    /**
     * Prints the live fleet dashboard.
     */
    public void printDashboard() {
        System.out.println(dashboardService.generateDashboardSummary());
    }

    // ═══════════════════════════════════════════════════════════
    // EVENT SUBSCRIPTIONS (for external subsystems)
    // ═══════════════════════════════════════════════════════════

    /**
     * Allows external subsystems to subscribe to delivery events.
     */
    public void subscribeToEvents(DeliveryEventType eventType, DeliveryEventListener listener) {
        eventManager.subscribe(eventType, listener);
    }

    /**
     * Gets the event manager for advanced subscription management.
     */
    public DeliveryEventManager getEventManager() {
        return eventManager;
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS for internal state
    // ═══════════════════════════════════════════════════════════

    public Map<String, Order> getOrders() { return Collections.unmodifiableMap(orders); }
    public Map<String, Customer> getCustomers() { return Collections.unmodifiableMap(customers); }
    public Map<String, Rider> getRiders() { return Collections.unmodifiableMap(riders); }
    public NotificationService getNotificationService() { return notificationService; }
}
