# 📚 API Reference

## Real-Time Delivery Monitoring System - Complete API Documentation

---

## 🏢 Core Facade API

### `DeliveryMonitoringFacade`

The main entry point for all delivery monitoring operations.

#### Key Methods
```java
// Customer & Rider Management
Customer registerCustomer(String name, String email, String phone)
Rider registerRider(String name, String phone, String vehicleType)
Device registerDeviceForRider(Rider rider)

// Order Lifecycle
Order createAndInitializeDelivery(String customerId, String pickupAddress,
                                 String dropoffAddress, Coordinate pickupCoord,
                                 Coordinate dropoffCoord)
void assignRiderToOrder(String orderId, String riderId)

// Real-Time Tracking
GPSPing processLocationUpdate(String deviceId, String orderId,
                             double latitude, double longitude)
void updateOrderStatus(String orderId, OrderStatus newStatus, String changedBy)

// Delivery Completion
PODRecord completeDelivery(String orderId, String signatureFile,
                           String photoFile, String notes)

// Query Methods
OrderStatus getOrderStatus(String orderId)
ETARecord getLatestETA(String orderId)
GPSPing getRiderPosition(String riderId)
String getTrackingURL(String orderId)
```

---

## 📞 Integration Support

- [`README.md`](../README.md) - Project overview
- [`integration.md`](../integration.md) - Partner integration guide  
- [`aboutus.md`](../aboutus.md) - System architecture

**API Version**: 1.0.0  
**Status**: ✅ Production Ready  
**Team**: Ramen Noodles 🍜