# 🤝 Contributing to Real-Time Delivery Monitoring

First off, thank you for considering contributing to the Real-Time Delivery Monitoring System! It's people like you that make this system such a great project.

Where you can contribute:

### 1. 🐛 Bug Reports & Fixes
- Find a bug in our ETA calculation? GPS pings failing under load?
- Please create an issue first describing the bug.
- Submit a Pull Request with the fix, making sure to include tests!

### 2. ✨ New Features
We're specifically looking for help with:
- **Dashboard UI Enhancements**: Our Fleet Dashboard could use real-time visual maps instead of text logs.
- **Advanced Route Planning**: Integrating third-party traffic APIs (like Google Maps or Mapbox).
- **More Integration Channels**: Currently we have SMS/Email via Strategy pattern, but push notifications via WebSocket could be a great addition.

### 3. 🧪 Testing & Quality
- Increase unit test coverage (especially around edge cases).
- Load testing for our GPS tracking ingestion (aiming for 10ms processing at 10k+ trackers).
- Expanding our Exception Handling tests.

### 4. 📝 Documentation
- Clarifying partner integration guides in `integration.md`.
- Expanding the Javadocs inside our core logic.

### 👨‍💻 Getting Started
1. Fork the repo and clone locally.
2. Ensure you have Java 17 and Maven 3.9+ installed.
3. Run `mvn clean package` to ensure everything builds correctly.
4. Create a feature branch (`git checkout -b feature/your-feature-name`).
5. Commit your changes and push up to your fork.
6. Open a Pull Request!

If you are a member of a Partner Team (CenterDiv, DEI Hires, VERTEX), you can also contribute to our integration interfaces in `src/main/java/com/ramennoodles/delivery/integration/`. 
