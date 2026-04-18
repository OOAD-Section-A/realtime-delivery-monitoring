# 🔗 Partner Integration Guide

## Real-Time Delivery Monitoring System - Team Ramen Noodles

---

## 📋 Integration Overview

This guide provides step-by-step instructions for partner teams to integrate with our **Real-Time Delivery Monitoring System**. Our system provides live tracking, GPS monitoring, ETA calculation, and delivery management capabilities.

### Integration Partner Teams
1. **VERTEX (Team #17)** - Order Fulfillment
2. **DEI Hires (Team #6)** - Delivery Orders  
3. **CenterDiv (Team #2)** - Transport & Logistics Management

---

## 🚀 Quick Integration

### Step 1: Add Our Integration JAR

```bash
# Add our integration JAR to your project
cp lib/ramen-noodles-delivery-monitoring.jar your_project/lib/

# Add to your classpath
javac -cp "lib/ramen-noodles-delivery-monitoring.jar" YourIntegration.java
java -cp "lib/ramen-noodles-delivery-monitoring.jar" YourMainClass
```

### Step 2: Import Required Interfaces

```java
import com.ramennoodles.delivery.facade.DeliveryMonitoringFacade;
import com.ramennoodles.delivery.integration.*;
import com.ramennoodles.delivery.model.*;
import com.ramennoodles.delivery.enums.*;
import com.ramennoodles.delivery.observer.*;
```

### Step 3: Initialize Our Facade

```java
// Create instance of our facade
DeliveryMonitoringFacade deliverySystem = new DeliveryMonitoringFacade();

// Register for events
deliverySystem.subscribeToEvents(DeliveryEventType.ORDER_DELIVERED, 
    (eventType, data) -> {
        String orderId = (String) data.get("orderId");
        System.out.println("Order delivered: " + orderId);
        // Handle in your system
    });
```

---

## 📦 TEAM-SPECIFIC INTEGRATION

### 1. VERTEX (Order Fulfillment) - Team #17

#### Your Integration Points
```java
public interface IOrderFulfillmentService {
    Order getOrderDetails(String orderId);
    List<Order> getDispatchedOrders();
    void notifyOrderPickedUp(String orderId, String riderId);
    void notifyOrderDelivered(String orderId, PODRecord pod);
}
```

#### Integration Code
```java
// When you complete order fulfillment, send to us
public void onOrderFulfilled(String orderId, String customerId, 
                            String pickupAddr, String dropoffAddr) {
    
    Coordinate pickup = new Coordinate(12.9352, 77.6245);
    Coordinate dropoff = new Coordinate(12.9716, 77.5946);
    
    Order order = deliverySystem.createAndInitializeDelivery(
        customerId, pickupAddr, dropoffAddr, pickup, dropoff
    );
}
```

#### Handle Events
```java
// Subscribe to delivery completion
deliverySystem.subscribeToEvents(DeliveryEventType.ORDER_DELIVERED,
    (eventType, data) -> {
        String orderId = (String) data.get("orderId");
        PODRecord pod = (PODRecord) data.get("pod");
        markOrderAsDelivered(orderId, pod);
    });
```

---

### 2. DEI Hires (Delivery Orders) - Team #6

#### Your Integration Points
```java
public interface IDeliveryOrderService {
    DeliveryOrder createDeliveryOrder(String customerId, String pickup, String dropoff);
    DeliveryOrder getDeliveryOrder(String orderId);
    Customer getCustomerDetails(String customerId);
    void updateOrderStatus(String orderId, OrderStatus status);
}
```

#### Integration Code
```java
// When customer places order, notify us
public void onOrderPlaced(String customerId, String pickup, String dropoff) {
    Customer customer = getCustomerFromYourSystem(customerId);
    
    deliverySystem.registerCustomer(customer.getName(), 
                                   customer.getEmail(), 
                                   customer.getPhone());
    
    Coordinate pickupCoord = geocodeAddress(pickup);
    Coordinate dropoffCoord = geocodeAddress(dropoff);
    
    Order order = deliverySystem.createAndInitializeDelivery(
        customerId, pickup, dropoff, pickupCoord, dropoffCoord
    );
}
```

#### Get Tracking Information
```java
// Provide tracking to customers
public String getTrackingUrl(String orderId) {
    return deliverySystem.getTrackingURL(orderId);
}

public OrderStatus getCurrentStatus(String orderId) {
    return deliverySystem.getOrderStatus(orderId);
}
```

---

### 3. CenterDiv (Transport & Logistics) - Team #2

#### Your Integration Points
```java
public interface ITransportLogisticsService {
    Rider getRiderDetails(String riderId);
    List<Rider> getAvailableRiders(String zone);
    RoutePlan calculateOptimalRoute(String pickup, String dropoff, List<String> waypoints);
    void reportVehicleHealth(String riderId, VehicleHealthReport report);
}
```

#### Integration Code
```java
// When we need riders, query your system
public void onRiderNeeded(String orderId, String zone) {
    List<Rider> availableRiders = getAvailableRidersFromYourSystem(zone);
    
    if (!availableRiders.isEmpty()) {
        Rider rider = availableRiders.get(0);
        deliverySystem.assignRiderToOrder(orderId, rider.getRiderId());
    }
}
```

#### Receive GPS Updates
```java
// Subscribe to location updates
deliverySystem.subscribeToEvents(DeliveryEventType.LOCATION_UPDATED,
    (eventType, data) -> {
        String riderId = (String) data.get("riderId");
        Double latitude = (Double) data.get("latitude");
        Double longitude = (Double) data.get("longitude");
        
        updateRiderLocation(riderId, latitude, longitude);
    });
```

---

## 📊 Available Events

### Event Types
```java
public enum DeliveryEventType {
    ORDER_CREATED,           // New order created
    RIDER_ASSIGNED,          // Rider assigned to order
    STATUS_CHANGED,          // Order status updated
    LOCATION_UPDATED,        // GPS location update
    GEOFENCE_ENTRY,          // Vehicle entered zone
    GEOFENCE_EXIT,           // Vehicle left zone
    ETA_UPDATED,             // ETA recalculated
    POD_SUBMITTED,           // Proof of delivery submitted
    ORDER_DELIVERED,         // Order successfully delivered
    ORDER_FAILED             // Delivery failed
}
```

### Event Subscription Example
```java
DeliveryEventListener myListener = (eventType, data) -> {
    switch(eventType) {
        case ORDER_DELIVERED:
            handleDeliveryCompletion(data);
            break;
        case LOCATION_UPDATED:
            handleLocationUpdate(data);
            break;
        case STATUS_CHANGED:
            handleStatusChange(data);
            break;
    }
};

deliverySystem.subscribeToEvents(DeliveryEventType.ORDER_DELIVERED, myListener);
deliverySystem.subscribeToEvents(DeliveryEventType.LOCATION_UPDATED, myListener);
```

---

## 🔧 Key API Methods

### Order Management
```java
Order createAndInitializeDelivery(String customerId, String pickupAddress, 
                                 String dropoffAddress, Coordinate pickupCoord, 
                                 Coordinate dropoffCoord)
void assignRiderToOrder(String orderId, String riderId)
```

### Tracking & Monitoring
```java
GPSPing processLocationUpdate(String deviceId, String orderId, 
                             double latitude, double longitude)
void updateOrderStatus(String orderId, OrderStatus newStatus, String changedBy)
```

### Query Methods
```java
OrderStatus getOrderStatus(String orderId)
ETARecord getLatestETA(String orderId)
GPSPing getRiderPosition(String riderId)
PODRecord getPOD(String orderId)
String getTrackingURL(String orderId)
```

---

## ⚠️ Error Handling

Our system implements comprehensive exception handling:

```java
try {
    Order order = deliverySystem.getOrder(orderId);
} catch (Exception e) {
    // Exception automatically logged and handled
    // Your system can continue gracefully
}
```

### Exception Categories
- **Input/Validation Errors** - Invalid data, missing fields
- **Connectivity Errors** - Network timeouts, service unavailability
- **Resource Errors** - Missing resources, capacity exceeded
- **System Errors** - Platform failures, processing errors

---

## 🧪 Integration Testing

### Test Your Integration
```java
public class IntegrationTest {
    public static void main(String[] args) {
        DeliveryMonitoringFacade deliverySystem = new DeliveryMonitoringFacade();
        
        // Register your listeners
        deliverySystem.subscribeToEvents(DeliveryEventType.ORDER_DELIVERED,
            (eventType, data) -> System.out.println("✅ Integration works!"));
        
        // Test basic operations
        Customer customer = deliverySystem.registerCustomer("Test", "test@email.com", "+91-9999999999");
        Rider rider = deliverySystem.registerRider("Test Rider", "+91-8888888888", "Motorcycle");
        
        System.out.println("✅ Integration test passed!");
    }
}
```

---

## 📞 Support & Contact

### Integration Support
- **Technical Issues**: Contact team members via GitHub issues
- **Integration Help**: See [`docs/API.md`](docs/API.md) for complete API reference
- **Testing Assistance**: Use provided test harness for validation

### Integration Checklist
- [ ] Add our JAR to your classpath
- [ ] Import required interfaces
- [ ] Initialize our facade
- [ ] Subscribe to relevant events
- [ ] Implement required interface methods
- [ ] Test data exchange
- [ ] Verify exception handling

---

## 🎯 Integration Success Metrics

### Successful Integration Indicators
- ✅ Events received in real-time
- ✅ Data exchange working bidirectionally
- ✅ Exception handling graceful
- ✅ Performance acceptable (< 100ms response)
- ✅ No data loss or corruption

---

**Last Updated**: 2026-04-19  
**Version**: 1.0.0  
**Team**: Ramen Noodles 🍜  
**Status**: ✅ Ready for Partner Integration