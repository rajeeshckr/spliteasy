#!/usr/bin/env bash
# Deploy SplitEasy to NixOS with Podman

set -e

# Configuration
SERVER_IP="${1:-192.168.1.30}"
SPLITEASY_DIR="${2:-~/spliteasy}"

echo "=========================================="
echo "SplitEasy NixOS Deployment"
echo "=========================================="
echo "Server IP: $SERVER_IP"
echo "Project Directory: $SPLITEASY_DIR"
echo ""

# Update code
echo "1. Pulling latest changes..."
cd "$SPLITEASY_DIR"
git pull

# Build backend
echo ""
echo "2. Building backend image..."
sudo podman build -t localhost/spliteasy-backend:latest "$SPLITEASY_DIR/backend"

# Build web client with correct API URL
echo ""
echo "3. Building web client image with API URL..."
sudo podman build \
  --build-arg VITE_API_URL="http://${SERVER_IP}:9090" \
  -t localhost/spliteasy-webclient:latest \
  "$SPLITEASY_DIR/web-client"

# Restart services
echo ""
echo "4. Restarting services..."
sudo systemctl restart podman-spliteasy-backend.service
sudo systemctl restart podman-spliteasy-webclient.service

# Wait a bit for services to start
sleep 3

# Check status
echo ""
echo "5. Checking service status..."
echo ""
echo "Backend:"
sudo systemctl status podman-spliteasy-backend.service --no-pager -l | head -15
echo ""
echo "Web Client:"
sudo systemctl status podman-spliteasy-webclient.service --no-pager -l | head -15

# Verify containers are running
echo ""
echo "6. Running containers:"
sudo podman ps --filter name=spliteasy

echo ""
echo "=========================================="
echo "Deployment complete!"
echo "=========================================="
echo "Backend API: http://${SERVER_IP}:9090"
echo "Web Client:  http://${SERVER_IP}:3000"
echo ""
echo "Test API health:"
echo "  curl http://${SERVER_IP}:9090/"
echo ""
echo "View logs:"
echo "  sudo journalctl -u podman-spliteasy-backend -f"
echo "  sudo journalctl -u podman-spliteasy-webclient -f"
echo ""
