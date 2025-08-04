# RankForge Server

A simple Java-based web application for displaying CS2 player rankings that can be deployed on Google App Engine.

## Features

- **Modular Architecture**: Separate frontend and backend components
- **REST API**: JSON endpoints for player ranking data
- **Web Interface**: Responsive table view for player rankings
- **Google App Engine Ready**: Configured for GAE deployment
- **Spring Boot**: Modern Java web framework
- **Thymeleaf Templates**: Server-side rendering for the frontend

## Project Structure

```
rank-forge-server/
├── src/main/java/com/rankforge/server/
│   ├── RankForgeServerApplication.java    # Main Spring Boot application
│   ├── controller/
│   │   ├── api/                          # REST API controllers
│   │   └── web/                          # Web MVC controllers
│   ├── service/                          # Business logic layer
│   └── dto/                              # Data Transfer Objects
├── src/main/resources/
│   ├── templates/                        # Thymeleaf templates
│   ├── static/css/                       # CSS stylesheets
│   ├── application.properties            # Spring Boot configuration
│   └── application-gae.properties        # GAE-specific configuration
└── src/main/appengine/
    └── app.yaml                          # Google App Engine configuration
```

## API Endpoints

### REST API
- `GET /api/rankings` - Get all player rankings
- `GET /api/rankings/top?limit=N` - Get top N player rankings
- `GET /api/rankings/health` - Health check endpoint

### Web Interface
- `GET /` - Home page (redirects to rankings)
- `GET /rankings` - Player rankings table view
- `GET /rankings?limit=N` - Limited player rankings view

## Running Locally

1. Build the project:
   ```bash
   mvn clean compile
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. Access the application:
   - Web Interface: http://localhost:8080/
   - API: http://localhost:8080/api/rankings

## Deploying to Google App Engine

1. Install Google Cloud SDK and authenticate:
   ```bash
   gcloud auth login
   gcloud config set project YOUR_PROJECT_ID
   ```

2. Build and deploy:
   ```bash
   mvn clean package
   mvn appengine:deploy
   ```

3. View your deployed application:
   ```bash
   gcloud app browse
   ```

## Configuration

### Local Development
- Configuration in `application.properties`
- Thymeleaf caching disabled for development
- Debug logging enabled

### Google App Engine
- Configuration in `application-gae.properties`
- Optimized for production deployment
- Health checks configured
- Auto-scaling enabled

## Data Source

Currently uses mock data generated in `PlayerRankingService`. In a production environment, this would be replaced with:
- Database integration (using rank-forge-pipeline)
- Persistent storage for player statistics

## Dependencies

- **Spring Boot Web**: Web framework and REST API
- **Spring Boot Thymeleaf**: Server-side templating
- **rank-forge-core**: Core models and interfaces
- **Jackson**: JSON processing
- **Google App Engine API**: GAE integration

## Responsive Design

The web interface is fully responsive and includes:
- Mobile-friendly table layout
- Gradient background design
- Modern CSS styling with Inter font
- Hover effects and animations
- Top player highlighting with medals