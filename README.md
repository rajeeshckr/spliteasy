# SplitEasy - Expense Splitting Application

A full-stack expense-splitting application similar to Splitwise, with a Ktor backend and Android frontend.

## Architecture

```
┌─────────────────┐         HTTP/JSON          ┌──────────────────┐
│                 │◄───────────────────────────►│                  │
│  Android App    │      REST API @ :8080      │  Ktor Backend    │
│  (Jetpack       │                            │  (SQLite DB)     │
│   Compose)      │                            │                  │
└─────────────────┘                            └──────────────────┘
```

## Project Structure

```
Split Easy/
├── backend/                 # Ktor REST API Server
│   ├── src/main/kotlin/com/spliteasy/server/
│   │   ├── Application.kt  # Main entry point
│   │   ├── models/         # Exposed ORM table definitions
│   │   ├── dto/            # Data transfer objects
│   │   ├── service/        # Business logic layer
│   │   ├── routes/         # HTTP route handlers
│   │   └── plugins/        # Ktor plugins (DB, Auth, Routing)
│   └── data/               # SQLite database file
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
- **Authentication:** JWT (7-day expiry)
- **Password Hashing:** BCrypt
- **Serialization:** kotlinx-serialization
- **Build Tool:** Gradle 8.5 with Kotlin DSL
- **Java Version:** 17

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

### Test Users

The backend automatically seeds 5 test users on first launch:

| Username | Email              | Password   |
|----------|--------------------|------------|
| alice    | alice@test.com     | password1  |
| bob      | bob@test.com       | password2  |
| carol    | carol@test.com     | password3  |
| dave     | dave@test.com      | password4  |
| eve      | eve@test.com       | password5  |

### API Usage Examples

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

## Security Features

- **Password Hashing:** BCrypt with salt
- **JWT Tokens:** 7-day expiration
- **Protected Endpoints:** All non-auth endpoints require valid JWT
- **Authorization Checks:** Users can only access groups they're members of
- **CORS Enabled:** For development purposes (configure for production)

## Development Status

### ✅ Completed (Phase 1 - Backend)
- [x] Ktor server setup with Netty
- [x] SQLite database with Exposed ORM
- [x] All database models and tables
- [x] JWT authentication system
- [x] User registration and login
- [x] User search functionality
- [x] Group CRUD operations
- [x] Group member management
- [x] Expense creation with equal splitting
- [x] Balance calculation with debt simplification
- [x] Debt settlement tracking
- [x] Dashboard summary endpoint
- [x] Test user seeding
- [x] CORS configuration
- [x] Error handling
- [x] All endpoints tested and working

### ❌ TODO (Phase 2-4 - Android App)
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
