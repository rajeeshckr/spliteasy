# SplitEasy Development Guide

Complete guide for building, running, and deploying the SplitEasy application.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Building the Backend](#building-the-backend)
3. [Running the Backend on Linux](#running-the-backend-on-linux)
4. [Building the Android App](#building-the-android-app)
5. [Installing APK on Android](#installing-apk-on-android)
6. [Configuring Backend URL](#configuring-backend-url)
7. [Network Configuration](#network-configuration)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### For Backend Development
- **Java 17** (JDK 17)
- **Gradle 8.5+** (or use included wrapper)
- **Git** (for version control)
- Linux/macOS/Windows with bash shell

### For Android Development
- **Android Studio** (latest version)
- **Android SDK** (API 26+, target API 36)
- **Java 17** (same as backend)
- **Gradle 8.5+**

### For Deployment
- Linux server with public IP or domain name
- Port 8080 accessible from internet
- Android device or emulator

---

## Building the Backend

### 1. Clone the Repository
```bash
git clone <repository-url>
cd SplitEasy/backend
```

### 2. Verify Java Version
```bash
java -version
# Should show: openjdk version "17.x.x"

# If not Java 17, set JAVA_HOME:
export JAVA_HOME=/path/to/java-17
```

### 3. Build the Backend
```bash
# Clean build
./gradlew clean build

# Skip tests for faster build
./gradlew build -x test
```

**Expected Output:**
```
BUILD SUCCESSFUL in Xs
```

### 4. Run Backend Locally
```bash
./gradlew run
```

**Expected Output:**
```
Seeded 5 test users
Application started in X.XX seconds.
Responding at http://0.0.0.0:8080
```

### 5. Test Backend is Running
```bash
# In another terminal
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}'

# Should return JSON with token
```

---

## Running the Backend on Linux

### Option 1: Direct Execution (Development)

#### Step 1: Install Java 17
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

# Verify installation
java -version
```

#### Step 2: Copy Backend to Server
```bash
# From your local machine
scp -r backend/ user@your-server.com:~/SplitEasy/

# Or clone directly on server
ssh user@your-server.com
git clone <repository-url>
cd SplitEasy/backend
```

#### Step 3: Run Backend
```bash
# Make gradlew executable
chmod +x gradlew

# Run the server
./gradlew run

# To keep running after SSH disconnect:
nohup ./gradlew run > server.log 2>&1 &

# Check logs
tail -f server.log
```

#### Step 4: Configure Firewall
```bash
# Allow port 8080
sudo ufw allow 8080/tcp

# Or for firewalld
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

#### Step 5: Verify External Access
```bash
# From your local machine or another device
curl http://YOUR_SERVER_IP:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}'
```

---

### Option 2: Systemd Service (Production)

#### Step 1: Build Standalone JAR
```bash
cd backend
./gradlew build
./gradlew installDist

# JAR will be in build/install/spliteasy-backend/
```

#### Step 2: Create Systemd Service
```bash
sudo nano /etc/systemd/system/spliteasy.service
```

**Service file content:**
```ini
[Unit]
Description=SplitEasy Backend API
After=network.target

[Service]
Type=simple
User=spliteasy
WorkingDirectory=/home/spliteasy/backend
ExecStart=/usr/bin/java -jar /home/spliteasy/backend/build/libs/spliteasy-backend-1.0.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

# Environment
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk"

[Install]
WantedBy=multi-user.target
```

#### Step 3: Create Service User
```bash
sudo useradd -r -s /bin/false spliteasy
sudo chown -R spliteasy:spliteasy /home/spliteasy
```

#### Step 4: Start Service
```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable on boot
sudo systemctl enable spliteasy

# Start service
sudo systemctl start spliteasy

# Check status
sudo systemctl status spliteasy

# View logs
sudo journalctl -u spliteasy -f
```

#### Step 5: Manage Service
```bash
# Stop
sudo systemctl stop spliteasy

# Restart
sudo systemctl restart spliteasy

# Check logs
sudo journalctl -u spliteasy --since "10 minutes ago"
```

---

### Option 3: Docker (Recommended for Production)

#### Step 1: Create Dockerfile
```bash
cd backend
nano Dockerfile
```

**Dockerfile content:**
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY . .
RUN ./gradlew build -x test
EXPOSE 8080
CMD ["./gradlew", "run"]
```

#### Step 2: Build Docker Image
```bash
docker build -t spliteasy-backend:latest .
```

#### Step 3: Run Container
```bash
docker run -d \
  --name spliteasy-backend \
  -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  --restart unless-stopped \
  spliteasy-backend:latest

# View logs
docker logs -f spliteasy-backend
```

#### Step 4: Docker Compose (Optional)
```bash
nano docker-compose.yml
```

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    volumes:
      - ./backend/data:/app/data
    restart: unless-stopped
    environment:
      - JAVA_OPTS=-Xmx512m
```

```bash
# Start
docker-compose up -d

# Stop
docker-compose down

# Logs
docker-compose logs -f
```

---

### Option 4: NixOS with Podman (Recommended for NixOS Users)

NixOS users can run the backend using Podman, which is the preferred container runtime for declarative NixOS configurations.

#### Prerequisites for NixOS

Ensure Podman is enabled in your NixOS configuration:

**In your `configuration.nix`:**
```nix
{ config, pkgs, ... }:

{
  # Enable Podman
  virtualisation.podman = {
    enable = true;
    # Create a `docker` alias for podman, to use it as a drop-in replacement
    dockerCompat = true;
    # Enable Docker-compatible socket
    dockerSocket.enable = true;
    # Required for containers under podman-compose to be able to talk to each other
    defaultNetwork.settings.dns_enabled = true;
  };

  # Enable container tools
  environment.systemPackages = with pkgs; [
    podman-compose  # Docker Compose compatibility
    buildah         # Build OCI images
    skopeo          # Copy/inspect container images
  ];
}
```

**Apply the configuration:**
```bash
sudo nixos-rebuild switch
```

---

#### Step 1: Create Containerfile

```bash
cd backend
nano Containerfile  # Podman uses Containerfile, compatible with Dockerfile
```

**Containerfile content:**
```dockerfile
FROM docker.io/library/openjdk:17-slim

# Set working directory
WORKDIR /app

# Copy backend source
COPY . .

# Build the application
RUN ./gradlew build -x test

# Create data directory
RUN mkdir -p /app/data

# Expose port
EXPOSE 8080

# Run the application
CMD ["./gradlew", "run", "--no-daemon"]
```

---

#### Step 2: Build Container Image with Podman

```bash
cd backend

# Build the image
podman build -t spliteasy-backend:latest .

# Verify image was created
podman images | grep spliteasy
```

**Build with custom name:**
```bash
podman build -t localhost/spliteasy-backend:1.0.0 .
```

---

#### Step 3: Run Container with Podman

**Basic run:**
```bash
podman run -d \
  --name spliteasy-backend \
  -p 8080:8080 \
  localhost/spliteasy-backend:latest

# Check status
podman ps

# View logs
podman logs -f spliteasy-backend
```

**Run with persistent data volume:**
```bash
# Create a named volume
podman volume create spliteasy-data

# Run with volume mount
podman run -d \
  --name spliteasy-backend \
  -p 8080:8080 \
  -v spliteasy-data:/app/data:Z \
  --restart unless-stopped \
  localhost/spliteasy-backend:latest

# Inspect volume
podman volume inspect spliteasy-data
```

**Run with host directory mount:**
```bash
podman run -d \
  --name spliteasy-backend \
  -p 8080:8080 \
  -v $HOME/spliteasy-data:/app/data:Z \
  --restart unless-stopped \
  localhost/spliteasy-backend:latest
```

**Note:** The `:Z` flag is important for SELinux contexts on NixOS/Fedora.

---

#### Step 4: Managing the Container

```bash
# Stop container
podman stop spliteasy-backend

# Start container
podman start spliteasy-backend

# Restart container
podman restart spliteasy-backend

# View logs
podman logs spliteasy-backend
podman logs -f spliteasy-backend  # Follow logs

# Execute command in container
podman exec -it spliteasy-backend bash

# Remove container
podman rm -f spliteasy-backend

# Remove image
podman rmi localhost/spliteasy-backend:latest
```

---

#### Step 5: Push to Container Registry

##### Option A: Docker Hub

```bash
# Login to Docker Hub
podman login docker.io
# Enter username and password

# Tag image for Docker Hub
podman tag localhost/spliteasy-backend:latest docker.io/YOUR_USERNAME/spliteasy-backend:latest

# Push to Docker Hub
podman push docker.io/YOUR_USERNAME/spliteasy-backend:latest

# Pull on another machine
podman pull docker.io/YOUR_USERNAME/spliteasy-backend:latest
```

##### Option B: GitHub Container Registry (ghcr.io)

```bash
# Create personal access token on GitHub with write:packages scope
# Settings → Developer settings → Personal access tokens → Generate new token

# Login to GitHub Container Registry
echo YOUR_GITHUB_TOKEN | podman login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# Tag image
podman tag localhost/spliteasy-backend:latest ghcr.io/YOUR_GITHUB_USERNAME/spliteasy-backend:latest

# Push to GitHub Container Registry
podman push ghcr.io/YOUR_GITHUB_USERNAME/spliteasy-backend:latest

# Make image public (optional)
# Go to: https://github.com/users/YOUR_USERNAME/packages/container/spliteasy-backend/settings
# Change visibility to public

# Pull on another machine
podman pull ghcr.io/YOUR_GITHUB_USERNAME/spliteasy-backend:latest
```

##### Option C: GitLab Container Registry

```bash
# Login to GitLab
podman login registry.gitlab.com
# Username: your-gitlab-username
# Password: your-gitlab-personal-access-token

# Tag image
podman tag localhost/spliteasy-backend:latest registry.gitlab.com/YOUR_USERNAME/spliteasy-backend:latest

# Push
podman push registry.gitlab.com/YOUR_USERNAME/spliteasy-backend:latest
```

##### Option D: Private Registry

```bash
# Login to private registry
podman login your-registry.com
# Enter credentials

# Tag image
podman tag localhost/spliteasy-backend:latest your-registry.com/spliteasy-backend:latest

# Push
podman push your-registry.com/spliteasy-backend:latest
```

---

#### Step 6: Run from Registry

Once pushed, you can run directly from the registry:

```bash
# From Docker Hub
podman run -d \
  --name spliteasy-backend \
  -p 8080:8080 \
  docker.io/YOUR_USERNAME/spliteasy-backend:latest

# From GitHub Container Registry
podman run -d \
  --name spliteasy-backend \
  -p 8080:8080 \
  ghcr.io/YOUR_GITHUB_USERNAME/spliteasy-backend:latest
```

---

#### Step 7: Podman Compose (Docker Compose Compatible)

Create a `podman-compose.yml` or `docker-compose.yml`:

```yaml
version: '3.8'

services:
  backend:
    image: localhost/spliteasy-backend:latest
    container_name: spliteasy-backend
    ports:
      - "8080:8080"
    volumes:
      - spliteasy-data:/app/data:Z
    restart: unless-stopped
    environment:
      - JAVA_OPTS=-Xmx512m

volumes:
  spliteasy-data:
```

**Run with podman-compose:**
```bash
# Start services
podman-compose up -d

# View logs
podman-compose logs -f

# Stop services
podman-compose down

# Rebuild and start
podman-compose up -d --build
```

---

#### Step 8: Systemd Integration (NixOS/Podman Quadlet)

For a more NixOS-native approach, use systemd with Podman Quadlet:

**Create systemd service file:**
```bash
mkdir -p ~/.config/containers/systemd
nano ~/.config/containers/systemd/spliteasy-backend.container
```

**Content of `spliteasy-backend.container`:**
```ini
[Unit]
Description=SplitEasy Backend API
After=network-online.target

[Container]
Image=localhost/spliteasy-backend:latest
PublishPort=8080:8080
Volume=/home/YOUR_USER/spliteasy-data:/app/data:Z
Environment=JAVA_OPTS=-Xmx512m

[Service]
Restart=always
TimeoutStartSec=900

[Install]
WantedBy=multi-user.target default.target
```

**Enable and start:**
```bash
# Reload systemd user daemon
systemctl --user daemon-reload

# Start service
systemctl --user start spliteasy-backend

# Enable on boot
systemctl --user enable spliteasy-backend

# Check status
systemctl --user status spliteasy-backend

# View logs
journalctl --user -u spliteasy-backend -f
```

---

#### Step 9: Declarative NixOS Configuration

For a fully declarative setup, add to your NixOS configuration:

**Create a NixOS module (`/etc/nixos/spliteasy.nix` or in your flake):**

```nix
{ config, pkgs, ... }:

{
  virtualisation.oci-containers = {
    backend = "podman";
    containers = {
      spliteasy-backend = {
        image = "ghcr.io/YOUR_USERNAME/spliteasy-backend:latest";
        # Or build locally:
        # imageFile = pkgs.dockerTools.buildImage {
        #   name = "spliteasy-backend";
        #   tag = "latest";
        #   contents = [ pkgs.openjdk17 ];
        # };

        ports = [ "8080:8080" ];

        volumes = [
          "/var/lib/spliteasy/data:/app/data:Z"
        ];

        environment = {
          JAVA_OPTS = "-Xmx512m";
        };

        extraOptions = [
          "--restart=unless-stopped"
        ];
      };
    };
  };

  # Open firewall port
  networking.firewall.allowedTCPPorts = [ 8080 ];

  # Create data directory
  systemd.tmpfiles.rules = [
    "d /var/lib/spliteasy 0755 root root -"
    "d /var/lib/spliteasy/data 0755 root root -"
  ];
}
```

**Import in your main configuration:**

```nix
# In configuration.nix or flake.nix
imports = [
  ./spliteasy.nix
];
```

**Apply configuration:**
```bash
sudo nixos-rebuild switch
```

**Manage the service:**
```bash
# Status
sudo systemctl status podman-spliteasy-backend.service

# Logs
sudo journalctl -u podman-spliteasy-backend.service -f

# Restart
sudo systemctl restart podman-spliteasy-backend.service
```

---

#### Step 10: Building Optimized NixOS Container

For a more Nix-native approach, build the container with Nix:

**Create `container.nix`:**

```nix
{ pkgs ? import <nixpkgs> {} }:

pkgs.dockerTools.buildLayeredImage {
  name = "spliteasy-backend";
  tag = "latest";

  contents = with pkgs; [
    openjdk17
    gradle
    bash
    coreutils
  ];

  config = {
    Cmd = [ "${pkgs.gradle}/bin/gradle" "run" "--no-daemon" ];
    WorkingDir = "/app";
    ExposedPorts = {
      "8080/tcp" = {};
    };
  };
}
```

**Build with Nix:**
```bash
nix-build container.nix
# Outputs: result

# Load into podman
podman load < result

# Run
podman run -d -p 8080:8080 spliteasy-backend:latest
```

---

#### Podman Troubleshooting

##### Container won't start
```bash
# Check logs
podman logs spliteasy-backend

# Run interactively for debugging
podman run -it --rm localhost/spliteasy-backend:latest bash
```

##### Port already in use
```bash
# Find what's using port 8080
sudo lsof -i :8080

# Use different host port
podman run -d -p 8081:8080 localhost/spliteasy-backend:latest
```

##### Permission denied on volume
```bash
# Use :Z for SELinux context
-v $HOME/data:/app/data:Z

# Or run as non-root user
podman run --user 1000:1000 ...
```

##### Image push authentication failed
```bash
# Re-login
podman logout docker.io
podman login docker.io

# Check credentials
cat ~/.config/containers/auth.json
```

##### Container networking issues
```bash
# Inspect network
podman network ls
podman network inspect podman

# Recreate default network
podman network rm podman
podman network create podman
```

---

#### Quick Reference - Podman Commands

```bash
# Build
podman build -t spliteasy-backend:latest .

# Run
podman run -d --name spliteasy -p 8080:8080 spliteasy-backend:latest

# Manage
podman ps                                    # List running
podman ps -a                                 # List all
podman logs -f spliteasy                     # Follow logs
podman stop spliteasy                        # Stop
podman start spliteasy                       # Start
podman restart spliteasy                     # Restart
podman rm -f spliteasy                       # Remove container
podman rmi spliteasy-backend:latest          # Remove image

# Registry
podman login docker.io                       # Login
podman push docker.io/user/image:tag         # Push
podman pull docker.io/user/image:tag         # Pull
podman logout docker.io                      # Logout

# System
podman system prune -a                       # Clean unused
podman volume ls                             # List volumes
podman network ls                            # List networks
```

---

## Building the Android App

### Option 1: Using Android Studio (Recommended)

#### Step 1: Open Project
1. Launch Android Studio
2. File → Open → Navigate to `SplitEasy/android`
3. Wait for Gradle sync to complete

#### Step 2: Build APK
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Wait for build to complete
3. APK location: `android/app/build/outputs/apk/debug/app-debug.apk`

#### Step 3: Install Directly to Device
1. Connect Android device via USB
2. Enable USB Debugging on device
3. Run → Run 'app'
4. Select your device

---

### Option 2: Command Line Build

#### Step 1: Setup Environment
```bash
# Set Android SDK location
export ANDROID_HOME=~/Android/Sdk  # or your SDK path
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Set Java 17
export JAVA_HOME=/path/to/java-17
```

#### Step 2: Build Debug APK
```bash
cd android
chmod +x gradlew

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# APK location:
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

#### Step 3: Build Release APK (Signed)
```bash
# Generate keystore (first time only)
keytool -genkey -v \
  -keystore spliteasy-release.keystore \
  -alias spliteasy \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Add to gradle.properties
echo "RELEASE_STORE_FILE=../spliteasy-release.keystore" >> gradle.properties
echo "RELEASE_STORE_PASSWORD=your_password" >> gradle.properties
echo "RELEASE_KEY_ALIAS=spliteasy" >> gradle.properties
echo "RELEASE_KEY_PASSWORD=your_password" >> gradle.properties

# Build release APK
./gradlew assembleRelease

# APK location:
ls -lh app/build/outputs/apk/release/app-release.apk
```

---

## Installing APK on Android

### Method 1: USB Installation (ADB)

#### Step 1: Enable USB Debugging
On Android device:
1. Settings → About Phone
2. Tap "Build Number" 7 times to enable Developer Options
3. Settings → Developer Options → Enable "USB Debugging"

#### Step 2: Connect Device
```bash
# Connect device via USB cable

# Verify connection
adb devices
# Should show: List of devices attached
#              XXXXXXXXXX    device

# If unauthorized, check phone for prompt
```

#### Step 3: Install APK
```bash
cd android/app/build/outputs/apk/debug

# Install
adb install app-debug.apk

# Or force reinstall
adb install -r app-debug.apk

# Install on specific device (if multiple connected)
adb -s DEVICE_ID install app-debug.apk
```

#### Step 4: Launch App
```bash
# Launch app directly
adb shell am start -n com.spliteasy.app/.MainActivity

# Or manually open from device
```

---

### Method 2: Wireless Installation (ADB over WiFi)

#### Step 1: Connect via USB First
```bash
# Connect device via USB
adb devices

# Enable TCP/IP mode on port 5555
adb tcpip 5555
```

#### Step 2: Get Device IP Address
On Android device:
1. Settings → About Phone → Status → IP Address
2. Note the IP (e.g., 192.168.1.100)

#### Step 3: Connect Wirelessly
```bash
# Disconnect USB cable

# Connect to device
adb connect 192.168.1.100:5555

# Verify
adb devices
# Should show: 192.168.1.100:5555    device

# Install APK
adb install app-debug.apk
```

#### Step 4: Disconnect
```bash
adb disconnect 192.168.1.100:5555
```

---

### Method 3: Direct Download (No Computer)

#### Step 1: Host APK on Web Server
```bash
# On your server
cd /var/www/html
sudo cp /path/to/app-debug.apk .
sudo chmod 644 app-debug.apk

# Or use Python
cd /path/to/apk
python3 -m http.server 8000
```

#### Step 2: Download on Device
1. Open browser on Android device
2. Navigate to: `http://YOUR_SERVER_IP:8000/app-debug.apk`
3. Download APK

#### Step 3: Install
1. Open Downloads folder
2. Tap on `app-debug.apk`
3. Allow "Install from Unknown Sources" if prompted
4. Tap "Install"

---

### Method 4: File Transfer

#### Via Google Drive/Dropbox:
1. Upload APK to cloud storage
2. Download on Android device
3. Install from Downloads

#### Via Email:
1. Email APK as attachment
2. Download on Android device
3. Install from Downloads

---

## Configuring Backend URL

The Android app has built-in server URL configuration. You can point it to any backend server using DNS name or IP address.

### Configuration via App Settings

#### Step 1: Access Settings
1. Open SplitEasy app
2. Login screen → Tap Settings icon (⚙️) in top right
   OR
3. Dashboard → Tap Settings icon (⚙️) in top right

#### Step 2: Configure Server URL
1. Settings → Server Configuration
2. Enter backend URL:
   - **For emulator:** `http://10.0.2.2:8080` (localhost on host machine)
   - **For physical device (same WiFi):** `http://192.168.1.X:8080`
   - **For remote server (IP):** `http://YOUR_SERVER_IP:8080`
   - **For remote server (DNS):** `http://api.spliteasy.com:8080`
   - **For HTTPS:** `https://api.spliteasy.com`

3. Tap "Save"
4. Configuration saved to device storage

#### Supported URL Formats
```
✅ http://10.0.2.2:8080
✅ http://192.168.1.100:8080
✅ http://example.com:8080
✅ http://api.example.com:8080
✅ https://api.example.com
✅ https://api.example.com:443
❌ example.com (missing protocol)
❌ http://example.com:8080/ (trailing slash - will be trimmed)
```

---

### Configuration via Code (Development)

If you need to change the default URL in the code:

#### File: `android/app/src/main/java/com/spliteasy/app/data/api/ApiClient.kt`
```kotlin
object ApiClient {
    // Change this default URL
    private const val DEFAULT_BASE_URL = "http://YOUR_DNS_NAME:8080"

    // Rest of code...
}
```

#### File: `android/app/src/main/java/com/spliteasy/app/data/TokenManager.kt`
```kotlin
val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
    // Change this default URL
    prefs[SERVER_URL_KEY] ?: "http://YOUR_DNS_NAME:8080"
}
```

After changing, rebuild the APK:
```bash
cd android
./gradlew clean assembleDebug
```

---

## Network Configuration

### Setting up DNS for Backend

#### Option 1: Public Domain with DNS Provider

1. **Buy a domain** (e.g., from Namecheap, GoDaddy)
2. **Add A Record** pointing to your server IP:
   ```
   Type: A
   Host: api
   Value: YOUR_SERVER_IP
   TTL: 3600
   ```
3. **Test DNS resolution:**
   ```bash
   nslookup api.yourdomain.com
   ping api.yourdomain.com
   ```
4. **Use in app:** `http://api.yourdomain.com:8080`

---

#### Option 2: Dynamic DNS (Free)

If your server has dynamic IP:

1. **Sign up** for free DDNS (e.g., No-IP, DuckDNS)
2. **Create hostname:** `spliteasy.ddns.net`
3. **Install DDNS client** on server:
   ```bash
   # For DuckDNS
   echo url="https://www.duckdns.org/update?domains=spliteasy&token=YOUR_TOKEN" | curl -k -o ~/duckdns/duck.log -K -

   # Add to crontab
   crontab -e
   */5 * * * * ~/duckdns/duck.sh >/dev/null 2>&1
   ```
4. **Use in app:** `http://spliteasy.ddns.net:8080`

---

#### Option 3: Reverse Proxy with Nginx (Recommended)

Setup Nginx to handle HTTPS and proxy to backend:

##### Step 1: Install Nginx
```bash
sudo apt update
sudo apt install nginx
```

##### Step 2: Configure Nginx
```bash
sudo nano /etc/nginx/sites-available/spliteasy
```

**Configuration:**
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

##### Step 3: Enable Site
```bash
sudo ln -s /etc/nginx/sites-available/spliteasy /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

##### Step 4: Add HTTPS with Let's Encrypt
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

##### Step 5: Use in App
```
https://api.yourdomain.com
```

**Note:** Update AndroidManifest.xml to allow cleartext traffic if using HTTP:
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

---

### Port Forwarding (Home Network)

If running backend on home network:

#### Step 1: Find Local IP
```bash
hostname -I
# e.g., 192.168.1.100
```

#### Step 2: Configure Router
1. Access router admin panel (usually http://192.168.1.1)
2. Find "Port Forwarding" or "Virtual Server"
3. Add rule:
   ```
   Service: SplitEasy
   External Port: 8080
   Internal IP: 192.168.1.100
   Internal Port: 8080
   Protocol: TCP
   ```
4. Save and apply

#### Step 3: Find Public IP
```bash
curl ifconfig.me
# Note your public IP
```

#### Step 4: Use in App
```
http://YOUR_PUBLIC_IP:8080
```

---

### Testing Network Connectivity

#### From Android Device
```bash
# Test DNS resolution
nslookup api.yourdomain.com

# Test port connectivity (using Termux)
curl -I http://api.yourdomain.com:8080
```

#### From Server
```bash
# Check if backend is listening
netstat -tulpn | grep 8080

# Test local connection
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}'

# Check firewall
sudo ufw status
```

#### Network Debugging
```bash
# On server - monitor connections
sudo tcpdump -i any port 8080

# On device - test with curl
curl -v http://YOUR_SERVER:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}'
```

---

## Troubleshooting

### Backend Issues

#### Build Fails
```bash
# Clear Gradle cache
./gradlew clean --no-daemon

# Delete .gradle folder
rm -rf .gradle build

# Rebuild
./gradlew build
```

#### Wrong Java Version
```bash
# Check current version
java -version

# Set Java 17
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

#### Port 8080 Already in Use
```bash
# Find process using port
sudo lsof -i :8080

# Kill process
kill -9 PID

# Or change port in Application.kt
embeddedServer(Netty, port = 8081, ...)
```

#### Database Issues
```bash
# Remove database to start fresh
rm backend/data/spliteasy.db

# Restart server (will recreate with test users)
./gradlew run
```

---

### Android Build Issues

#### SDK Not Found
```bash
# Set Android SDK path
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
```

#### Gradle Sync Failed
```bash
# In Android Studio
File → Invalidate Caches / Restart

# Command line
./gradlew --stop
./gradlew clean
./gradlew build
```

#### APK Install Failed
```bash
# Uninstall existing app first
adb uninstall com.spliteasy.app

# Reinstall
adb install -r app-debug.apk
```

---

### Network Issues

#### App Can't Connect to Backend

1. **Check backend is running:**
   ```bash
   curl http://localhost:8080/api/auth/login
   ```

2. **Check firewall:**
   ```bash
   sudo ufw status
   sudo ufw allow 8080/tcp
   ```

3. **Check device can reach server:**
   ```bash
   # From device browser, visit:
   http://YOUR_SERVER_IP:8080
   ```

4. **Check server URL in app:**
   - Settings → Verify URL is correct
   - Try with IP instead of DNS
   - Ensure no trailing slash

5. **Check AndroidManifest.xml:**
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <application android:usesCleartextTraffic="true" ...>
   ```

#### DNS Not Resolving
```bash
# Test DNS
nslookup api.yourdomain.com
ping api.yourdomain.com

# Try with IP directly
http://SERVER_IP:8080
```

#### HTTPS Certificate Errors
```bash
# Check certificate
openssl s_client -connect api.yourdomain.com:443

# Renew Let's Encrypt
sudo certbot renew
```

---

### Runtime Issues

#### Token Expired
- Tokens expire after 7 days
- Logout and login again
- Token will be refreshed automatically

#### Database Locked
```bash
# Stop all backend instances
pkill -f gradle

# Remove lock
rm backend/data/spliteasy.db-lock

# Restart
./gradlew run
```

#### Memory Issues
```bash
# Increase JVM memory
export GRADLE_OPTS="-Xmx2g -XX:MaxPermSize=512m"
./gradlew run

# Or in gradle.properties
org.gradle.jvmargs=-Xmx2048m
```

---

## Quick Reference

### Common Commands

#### Backend
```bash
cd backend
./gradlew clean build          # Build
./gradlew run                  # Run
./gradlew test                 # Test
./gradlew build -x test        # Build without tests
```

#### Android
```bash
cd android
./gradlew clean                # Clean
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew installDebug         # Build and install
```

#### ADB
```bash
adb devices                    # List devices
adb install app-debug.apk      # Install APK
adb uninstall com.spliteasy.app # Uninstall
adb logcat                     # View logs
adb shell                      # Shell access
```

### Default Credentials

```
Username: alice
Password: password1

Other test users: bob, carol, dave, eve (password2-5)
```

### Default URLs

```
Emulator: http://10.0.2.2:8080
Local network: http://192.168.1.X:8080
```

---

## Support

For issues or questions:
1. Check this development guide
2. Review README.md
3. Check BLOCKERS.md for known issues
4. Check iteration completion docs for implementation details

---

**Last Updated:** February 7, 2026
**Version:** 1.0.0
