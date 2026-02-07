# SplitEasy Backend - Container Guide

Quick reference for building and running the SplitEasy backend in containers.

## Quick Start

### Build
```bash
cd backend
podman build -t spliteasy-backend:latest .
```

### Run
```bash
podman run -d \
  --name spliteasy-backend \
  -p 8080:8080 \
  -v spliteasy-data:/app/data:Z \
  --restart unless-stopped \
  spliteasy-backend:latest
```

### View Logs
```bash
podman logs -f spliteasy-backend
```

### Test
```bash
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}'
```

## Push to Registry

### Docker Hub
```bash
podman login docker.io
podman tag spliteasy-backend:latest docker.io/YOUR_USERNAME/spliteasy-backend:latest
podman push docker.io/YOUR_USERNAME/spliteasy-backend:latest
```

### GitHub Container Registry
```bash
echo YOUR_TOKEN | podman login ghcr.io -u YOUR_USERNAME --password-stdin
podman tag spliteasy-backend:latest ghcr.io/YOUR_USERNAME/spliteasy-backend:latest
podman push ghcr.io/YOUR_USERNAME/spliteasy-backend:latest
```

### GitLab Container Registry
```bash
podman login registry.gitlab.com
podman tag spliteasy-backend:latest registry.gitlab.com/YOUR_USERNAME/spliteasy-backend:latest
podman push registry.gitlab.com/YOUR_USERNAME/spliteasy-backend:latest
```

## Development

### Build with cache disabled
```bash
podman build --no-cache -t spliteasy-backend:latest .
```

### Run with host network (for debugging)
```bash
podman run --rm -it --network=host spliteasy-backend:latest
```

### Run with custom port
```bash
podman run -d -p 9090:8080 spliteasy-backend:latest
```

### Interactive shell in container
```bash
podman exec -it spliteasy-backend bash
```

## Notes

- Database is stored in `/app/data` inside the container
- Use volumes (`-v`) to persist data between container restarts
- Default credentials: alice/password1, bob/password2, etc.
- Backend listens on port 8080 inside the container

For complete documentation, see `../DEVELOPMENT.md`
