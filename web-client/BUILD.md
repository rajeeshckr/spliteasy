# Building SplitEasy Web Client

## Local Development
```bash
npm install
npm run dev
```

## Building Docker/Podman Image

### For localhost access (default):
```bash
sudo podman build -t localhost/spliteasy-webclient:latest ~/spliteasy/web-client
```

### For remote access (e.g., from other devices on network):
```bash
# Replace 192.168.1.30 with your server's IP address
sudo podman build \
  --build-arg VITE_API_URL=http://192.168.1.30:9090 \
  -t localhost/spliteasy-webclient:latest \
  ~/spliteasy/web-client
```

## After Building
Restart the service to use the new image:
```bash
sudo systemctl restart podman-spliteasy-webclient.service
```

Clear browser cache or use hard refresh (Ctrl+Shift+R) to load the new version.

## Troubleshooting

If the web client can't connect to the backend:
1. Check browser console for API errors
2. Verify the API URL in browser console:
   ```javascript
   localStorage.getItem('serverUrl')
   ```
3. Manually set the API URL (temporary fix):
   ```javascript
   localStorage.setItem('serverUrl', 'http://192.168.1.30:9090');
   location.reload();
   ```
