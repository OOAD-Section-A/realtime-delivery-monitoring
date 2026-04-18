# 📦 External Dependencies

## Directory Contents

This directory contains external JAR files and configuration files required for the Real-Time Delivery Monitoring System.

---

## 🔗 Integration JARs

### database-module-1.0.0-SNAPSHOT-standalone.jar
**Purpose**: SCM Database Module (from Database Team #15)  
**Version**: 1.0.0-SNAPSHOT  
**Usage**: Database integration with graceful fallback

### ramen-noodles-delivery-monitoring.jar (Build with build-jar.bat)
**Purpose**: Integration JAR for partner teams  
**Usage**: Add to partner project classpath

---

## ⚙️ Configuration Files

### database.properties
**Purpose**: Database connection configuration  
**Default**: MySQL with H2 fallback

---

## 📋 Integration Instructions

### For Partner Teams

1. **Add our JAR to your classpath**:
```bash
javac -cp "lib/ramen-noodles-delivery-monitoring.jar" YourCode.java
java -cp "lib/ramen-noodles-delivery-monitoring.jar" YourMain
```

2. **Import our facade**:
```java
import com.ramennoodles.delivery.facade.DeliveryMonitoringFacade;
```

---

## 🔧 Building Integration JAR

Run `build-jar.bat` in project root to create the integration JAR.

---

## 📞 Support

- [`README.md`](../README.md) - Project overview
- [`integration.md`](../integration.md) - Partner integration guide
- [`docs/API.md`](../docs/API.md) - Complete API reference