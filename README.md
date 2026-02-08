# SplitEasy - Expense Splitting Application

A full-stack expense-splitting application similar to Splitwise, with a Ktor backend, React web client, and Android frontend (planned).

## Architecture

```
┌─────────────────┐         HTTP/JSON          ┌──────────────────┐
│   Web Client    │◄───────────────────────────►│                  │
│   (React +      │      REST API @ :8080      │  Ktor Backend    │
│    Material UI) │                            │  (SQLite DB)     │
└─────────────────┘                            │                  │
                                               │                  │
┌─────────────────┐                            │                  │
│  Android App    │◄───────────────────────────►│                  │
│  (Jetpack       │      REST API @ :8080      │                  │
│   Compose)      │                            │                  │
└─────────────────┘                            └──────────────────┘
```

## Project Structure

```
spliteasy/
├── backend/                 # Ktor REST API Server
│   ├── src/main/kotlin/com/spliteasy/server/
│   │   ├── Application.kt  # Main entry point
│   │   ├── models/         # Exposed ORM table definitions
│   │   ├── dto/            # Data transfer objects
│   │   ├── service/        # Business logic layer
│   │   ├── routes/         # HTTP route handlers
│   │   └── plugins/        # Ktor plugins (DB, Auth, Routing)
│   ├── data/               # SQLite database file
│   └── Containerfile       # Docker/Podman image build
├── web-client/             # React Web Application ✅
│   ├── src/
│   │   ├── components/     # Reusable React components
│   │   ├── pages/          # Route pages (Dashboard, Groups, etc.)
│   │   ├── api/            # API client with Axios
│   │   ├── context/        # React Context (Auth)
│   │   └── hooks/          # Custom React hooks
│   ├── Containerfile       # Nginx static file serving
│   └── package.json        # NPM dependencies
├── deploy-nixos.sh         # Automated deployment script
├── e2e-tests.sh            # Comprehensive E2E test suite
└── android/                # Android App (TODO)
    └── app/
        └── src/main/java/com/spliteasy/app/
            ├── data/       # API client, DTOs, repositories
            ├── presentation/ # UI screens (Compose)
            └── ui/theme/   # Material 3 theming
```

## Tech Stack

### Backend
- **Language:** Kotlin
- **Framework:** Ktor 2.3.7 (Netty engine)
- **Database:** SQLite with Exposed ORM
- **Authentication:** JWT (24-hour expiry)
- **Password Hashing:** BCrypt
- **Serialization:** kotlinx-serialization
- **Build Tool:** Gradle 8.5 with Kotlin DSL
- **Java Version:** 17
- **Containerization:** Docker/Podman with multi-stage builds

### Web Client ✅
- **Language:** TypeScript
- **Framework:** React 18 with Vite
- **UI Library:** Material-UI (MUI) v5
- **HTTP Client:** Axios with interceptors
- **State Management:** React Context API
- **Routing:** React Router v6
- **Build Tool:** Vite 5
- **Deployment:** Nginx (static files in container)

### Android App (Planned)
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **HTTP Client:** Retrofit2 + OkHttp3
- **Architecture:** MVVM
- **Navigation:** Jetpack Navigation Compose
- **Local Storage:** DataStore (for auth tokens)
- **minSdk:** 26, **targetSdk:** 34

## Features Implemented

### Backend API ✅

#### Authentication
- **POST /api/auth/register** - Create new user account
- **POST /api/auth/login** - Login and receive JWT token

#### Users
- **GET /api/users/search?q={query}** - Search users by username/email

#### Groups
- **GET /api/groups** - List all groups for authenticated user
- **POST /api/groups** - Create a new group
- **GET /api/groups/{id}** - Get group details with members
- **POST /api/groups/{id}/members** - Add member to group
- **DELETE /api/groups/{id}/members/{userId}** - Remove member from group

#### Expenses
- **GET /api/groups/{id}/expenses** - List all expenses in a group
- **POST /api/groups/{id}/expenses** - Create expense (automatically splits equally among all members)
- **DELETE /api/groups/{id}/expenses/{expenseId}** - Delete an expense

#### Balances
- **GET /api/groups/{id}/balances** - Get simplified debts for a group
- **POST /api/groups/{id}/settle** - Record a debt settlement

#### Dashboard
- **GET /api/dashboard** - Get summary of all groups and balances

## Getting Started

### Prerequisites
- Java 17
- Gradle 8.5+

### Running the Backend

1. **Navigate to backend directory:**
   ```bash
   cd backend
   ```

2. **Start the server:**
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   ./gradlew run
   ```

3. **Server will start on:** `http://localhost:8080`

4. **Build only (without running):**
   ```bash
   ./gradlew build
   ```

### Running the Web Client

#### Development Mode

1. **Navigate to web-client directory:**
   ```bash
   cd web-client
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```

4. **Open in browser:** `http://localhost:5173`

The web client will automatically connect to the backend at `http://localhost:9090` (configurable).

#### Production Build (Docker/Podman)

**For local development:**
```bash
sudo podman build -t localhost/spliteasy-webclient:latest web-client/
```

**For remote access (e.g., from other devices on network):**
```bash
# Replace 192.168.1.30 with your server's IP
sudo podman build \
  --build-arg VITE_API_URL=http://192.168.1.30:9090 \
  -t localhost/spliteasy-webclient:latest \
  web-client/
```

**Run the container:**
```bash
sudo podman run -d -p 3000:80 --name spliteasy-web localhost/spliteasy-webclient:latest
```

### Deploying to NixOS/Podman

An automated deployment script is provided for NixOS systems:

```bash
# SSH to your server
ssh user@your-server-ip

# Clone the repository
cd ~/spliteasy
git pull

# Run the deployment script
./deploy-nixos.sh [SERVER_IP] [SPLITEASY_DIR]
```

The script will:
- Build both backend and web client images
- Configure the web client with the correct API URL
- Restart systemd services
- Display status and helpful commands

### Test Users

The backend automatically seeds 5 test users on first launch:

| Username | Email              | Password   |
|----------|--------------------|------------|
| alice    | alice@test.com     | password1  |
| bob      | bob@test.com       | password2  |
| carol    | carol@test.com     | password3  |
| dave     | dave@test.com      | password4  |
| eve      | eve@test.com       | password5  |

### Using the Application

#### Web Client (Recommended)

The easiest way to interact with SplitEasy is through the web interface:

1. **Start the backend:** `cd backend && ./gradlew run`
2. **Start the web client:** `cd web-client && npm run dev`
3. **Open browser:** `http://localhost:5173`
4. **Login with test user:** alice / password1

The web UI provides:
- User-friendly interface for all features
- Real-time balance calculations
- Group and expense management
- Settlement tracking
- Responsive design for mobile/desktop

#### API Usage Examples (Direct HTTP)

For API integration or testing, you can interact with the backend directly:

#### 1. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}'
```

Response:
```json
{
  "token": "eyJhbGc...",
  "userId": 1,
  "username": "alice"
}
```

#### 2. Create a Group
```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"name":"Weekend Trip","description":"Mountain hiking trip"}'
```

#### 3. Add Member to Group
```bash
curl -X POST http://localhost:8080/api/groups/1/members \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"userId":2}'
```

#### 4. Create an Expense
```bash
curl -X POST http://localhost:8080/api/groups/1/expenses \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"description":"Dinner","amountCents":5000,"paidByUserId":1}'
```

Note: Amount is in cents (5000 = $50.00). The expense will be split equally among all group members.

#### 5. Get Balances
```bash
curl -X GET http://localhost:8080/api/groups/1/balances \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

Response shows simplified debts:
```json
{
  "groupName": "Weekend Trip",
  "balances": [
    {
      "fromUser": {"id": 2, "username": "bob"},
      "toUser": {"id": 1, "username": "alice"},
      "amountCents": 2500
    }
  ]
}
```

#### 6. View Dashboard
```bash
curl -X GET http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## API Design Principles

1. **All monetary amounts in cents** - Stored and transmitted as Long integers to avoid floating-point precision issues
2. **JWT authentication** - All endpoints except auth require Bearer token
3. **Automatic expense splitting** - Expenses are automatically divided equally among all current group members
4. **Debt simplification** - Balance calculations minimize number of transactions needed
5. **RESTful design** - Standard HTTP methods and status codes

## Database Schema

### Users
- id (INTEGER PK)
- username (TEXT UNIQUE)
- email (TEXT UNIQUE)
- passwordHash (TEXT)
- createdAt (LONG)

### Groups
- id (INTEGER PK)
- name (TEXT)
- description (TEXT NULL)
- creatorId (INTEGER FK → Users)
- createdAt (LONG)

### GroupMembers
- groupId (INTEGER FK → Groups)
- userId (INTEGER FK → Users)
- joinedAt (LONG)
- PRIMARY KEY (groupId, userId)

### Expenses
- id (INTEGER PK)
- groupId (INTEGER FK → Groups)
- description (TEXT)
- amountCents (LONG)
- paidByUserId (INTEGER FK → Users)
- createdAt (LONG)

### ExpenseSplits
- id (INTEGER PK)
- expenseId (INTEGER FK → Expenses)
- userId (INTEGER FK → Users)
- shareAmountCents (LONG)
- settled (BOOLEAN)

### Settlements
- id (INTEGER PK)
- groupId (INTEGER FK → Groups)
- fromUserId (INTEGER FK → Users)
- toUserId (INTEGER FK → Users)
- amountCents (LONG)
- createdAt (LONG)

## Security Features

- **Password Hashing:** BCrypt with salt
- **JWT Tokens:** 24-hour expiration
- **Protected Endpoints:** All non-auth endpoints require valid JWT
- **Authorization Checks:** Users can only access groups they're members of
- **CORS Enabled:** For development purposes (configure for production)
- **Automatic Logout:** Web client logs out on 401 responses

## Development Status

### ✅ Completed (Phase 1 - Backend)
- [x] Ktor server setup with Netty
- [x] SQLite database with Exposed ORM
- [x] All database models and tables (including Settlements)
- [x] JWT authentication system with 24-hour expiry
- [x] User registration and login
- [x] User search functionality
- [x] Group CRUD operations
- [x] Group member management
- [x] Expense creation with equal splitting
- [x] Balance calculation with debt simplification
- [x] Partial and full settlement tracking
- [x] Dashboard summary endpoint
- [x] Test user seeding
- [x] CORS configuration
- [x] Error handling
- [x] Comprehensive E2E test suite (56 tests)
- [x] Docker/Podman containerization
- [x] All endpoints tested and working

### ✅ Completed (Phase 2 - Web Client)
- [x] React + TypeScript setup with Vite
- [x] Material-UI component library
- [x] Axios API client with interceptors
- [x] JWT authentication context
- [x] Login and registration pages
- [x] Dashboard with group overview
- [x] Group creation and detail pages
- [x] Member management UI
- [x] Expense creation and listing
- [x] Balance calculation display
- [x] Settlement recording UI
- [x] User search functionality
- [x] Responsive design
- [x] Error handling and loading states
- [x] Nginx containerization
- [x] Environment-based API URL configuration

### ❌ TODO (Phase 3-4 - Android App)
- [ ] Android project setup
- [ ] Retrofit API client
- [ ] Token management with DataStore
- [ ] Login screen
- [ ] Registration screen
- [ ] Dashboard screen
- [ ] Group creation and detail screens
- [ ] Expense creation screen
- [ ] Balance and settlement UI
- [ ] Member management UI
- [ ] Server URL configuration
- [ ] Material 3 theming
- [ ] Error handling and loading states
- [ ] APK build

## Troubleshooting

### Port 8080 already in use
```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9
```

### Database issues
```bash
# Remove database to start fresh
rm backend/data/spliteasy.db
# Test users will be re-seeded on next startup
```

### Java version issues
```bash
# Check Java version
java -version

# List available Java versions
/usr/libexec/java_home -V

# Set Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

## Future Enhancements

- [ ] Unequal expense splitting options
- [ ] Expense categories and tags
- [ ] Receipt image uploads
- [ ] Push notifications for new expenses
- [ ] Email notifications
- [ ] Multi-currency support
- [ ] Recurring expenses
- [ ] Export to PDF/CSV
- [ ] Group chat/comments
- [ ] Activity feed

## License

MIT License

## Author

Built with Claude Code using the Ralph Wiggum iterative development technique.
