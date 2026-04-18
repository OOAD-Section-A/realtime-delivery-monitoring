@echo off
REM Build Script for Ramen Noodles Delivery Monitoring Integration JAR

echo ╔══════════════════════════════════════════════════════════════╗
echo ║   🍜 Building Ramen Noodles Delivery Monitoring JAR          ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

REM Clean previous build
echo 🧹 Cleaning previous build...
if exist build rmdir /s /q build
mkdir build
mkdir build\META-INF

REM Copy source files
echo 📋 Copying source files...
xcopy /E /I src\main\java\com build\com > nul

REM Create manifest
echo 📝 Creating manifest...
echo Manifest-Version: 1.0 > build\META-INF\MANIFEST.MF
echo Main-Class: com.ramennoodles.delivery.facade.DeliveryMonitoringFacade >> build\META-INF\MANIFEST.MF

REM Create lib directory if needed
if not exist lib mkdir lib

echo.
echo ✅ Build structure created!
echo.
echo 📦 To create JAR: cd build && jar cvfm ..\lib\ramen-noodles-delivery-monitoring.jar META-INF\MANIFEST.MF com
echo.
echo 🎯 Integration JAR ready for partner teams!
pause