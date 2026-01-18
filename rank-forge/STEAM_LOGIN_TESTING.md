# Steam Login Testing Guide

## Prerequisites

1. **Steam API Key** (Required)
   - Go to https://steamcommunity.com/dev/apikey
   - Sign in with your Steam account
   - Register a new API key
   - Copy the key (you'll need it below)

2. **JWT Secret** (Required)
   - Generate a secure random secret:
     ```bash
     openssl rand -base64 32
     ```
   - Copy the output

3. **Database** (Optional - H2 will auto-create)
   - For local testing, H2 database will auto-create tables
   - Or use your existing database

## Step 1: Configure Backend

Navigate to the worktree:
```bash
cd .cursor/steam-login/rank-forge/rank-forge-server
```

Create or edit `src/main/resources/application-local.properties`:

```properties
# Steam Authentication Configuration
steam.api.key=YOUR_STEAM_API_KEY_HERE
steam.openid.realm=http://localhost:8080
steam.openid.return-url=http://localhost:8080/api/auth/callback
steam.openid.frontend-callback=http://localhost:5173/auth/callback

# JWT Configuration
jwt.secret=YOUR_JWT_SECRET_HERE
jwt.expiration=604800000

# Database (H2 for local testing - auto-creates tables)
spring.datasource.url=jdbc:h2:file:./data/rankforge-local;AUTO_SERVER=TRUE
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
```

**OR** use environment variables:
```bash
export STEAM_API_KEY="your_steam_api_key"
export JWT_SECRET="your_jwt_secret"
```

## Step 2: Start Backend Server

```bash
cd .cursor/steam-login/rank-forge/rank-forge-server
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Wait for: `Started RankForgeServerApplication`

Verify backend is running:
```bash
curl http://localhost:8080/api/auth/health
# Should return: "Auth API is healthy"
```

## Step 3: Start Frontend

Open a new terminal:

```bash
cd .cursor/steam-login/rank-forge/rank-forge-ui
npm install  # If first time
npm run dev
```

Frontend should start at: `http://localhost:5173`

## Step 4: Test Login Flow

### 4.1 Test Login Button

1. Open browser: `http://localhost:5173`
2. You should see a **"Login with Steam"** button in the top-right
3. Click it

### 4.2 Steam Authentication

1. You'll be redirected to Steam login page
2. Sign in with your Steam account
3. Steam will ask you to authorize the app
4. Click "Sign In" or "Allow"

### 4.3 Callback Handling

1. After Steam auth, you'll be redirected back to your app
2. The callback page should show "Completing login..."
3. Then redirect to `/my-profile`

### 4.4 Verify Login Success

After successful login, you should see:
- **User menu** in top-right (your Steam avatar + name)
- **My Profile** page showing your stats (if you have any)
- **Steam avatar** displayed

## Step 5: Test Protected Routes

1. **My Profile Page** (`/my-profile`)
   - Should show your Steam profile info
   - Should show your game stats (if available)
   - Should show your avatar

2. **User Menu**
   - Click your avatar/name in top-right
   - Should see dropdown with:
     - "My Profile"
     - "Steam Profile" (link to your Steam profile)
     - "Logout"

3. **Logout**
   - Click "Logout" from user menu
   - Should redirect to home
   - Login button should appear again

## Step 6: Test API Endpoints Directly

### Test Auth Endpoints

```bash
# Get login URL
curl http://localhost:8080/api/auth/login
# Returns: {"loginUrl":"https://steamcommunity.com/openid/login?..."}

# Health check
curl http://localhost:8080/api/auth/health
# Returns: "Auth API is healthy"
```

### Test User Endpoints (requires JWT token)

First, get your JWT token from browser localStorage:
1. Open browser DevTools (F12)
2. Go to Application → Local Storage → `http://localhost:5173`
3. Copy the `authToken` value

Then test:
```bash
# Get current user (replace YOUR_TOKEN)
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/users/me

# Get avatar URL
curl http://localhost:8080/api/users/YOUR_STEAM_ID64/avatar
```

## Troubleshooting

### Backend won't start

**Error: "Steam API key not configured"**
- Make sure `steam.api.key` is set in `application-local.properties`
- Or set `STEAM_API_KEY` environment variable

**Error: "JWT secret not configured"**
- Make sure `jwt.secret` is set
- Or set `JWT_SECRET` environment variable

**Error: Database connection failed**
- For H2, make sure `spring.jpa.hibernate.ddl-auto=update` is set
- H2 will auto-create the `Users` table

### Frontend issues

**Login button doesn't appear**
- Check browser console for errors
- Verify backend is running on port 8080
- Check that `AuthProvider` is wrapping the app in `App.tsx`

**Redirect loop after login**
- Check that `steam.openid.frontend-callback` matches your frontend URL
- Default should be `http://localhost:5173/auth/callback`

**Token not saved**
- Check browser console for errors
- Verify `/auth/callback` route exists in `App.tsx`
- Check that token is in URL query params: `?token=...`

### Steam OpenID issues

**"Invalid OpenID response"**
- Make sure `steam.openid.realm` matches your backend URL
- Should be `http://localhost:8080` for local testing
- Make sure `steam.openid.return-url` is correct

**Steam login page doesn't load**
- Check your internet connection
- Verify Steam is accessible
- Try in incognito mode (to rule out browser extensions)

## Expected Database State

After first login, check your database:

```sql
-- H2 Console: http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:file:./data/rankforge-local
-- Username: sa
-- Password: (empty)

SELECT * FROM Users;
```

You should see:
- Your Steam ID64
- Your Steam ID3 (format: `[U:1:xxx]`)
- Your persona name
- Avatar URLs
- Account creation date
- VAC ban status

## Quick Test Checklist

- [ ] Backend starts without errors
- [ ] Frontend starts without errors
- [ ] Login button appears in navbar
- [ ] Clicking login redirects to Steam
- [ ] Steam login works
- [ ] Callback redirects back to app
- [ ] User menu appears after login
- [ ] My Profile page loads
- [ ] Avatar displays correctly
- [ ] Logout works
- [ ] Protected routes require login

## Next Steps

Once basic login works:
1. Test with a Steam account that has game stats
2. Verify profile linking (Steam ID3 matches PlayerStats)
3. Test avatar display on other pages
4. Test token refresh
5. Test with multiple users

## Notes

- **First login**: User will be created in database automatically
- **Subsequent logins**: User data will be updated from Steam API
- **Token expiration**: JWT tokens expire after 7 days (configurable)
- **Database**: H2 database file is at `./data/rankforge-local.mv.db`
