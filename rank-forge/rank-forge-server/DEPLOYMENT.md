# RankForge Server - Deployment Guide

## Google App Engine Deployment

### Prerequisites

1. **Google Cloud SDK**: Install and configure the Google Cloud SDK
   ```bash
   # Install Google Cloud SDK
   curl https://sdk.cloud.google.com | bash
   exec -l $SHELL
   
   # Initialize and authenticate
   gcloud init
   gcloud auth login
   ```

2. **Java 17**: Ensure Java 17 is installed
   ```bash
   java -version
   ```

3. **Maven**: Ensure Maven is installed
   ```bash
   mvn -version
   ```

### Deployment Steps

1. **Set up Google Cloud Project**
   ```bash
   # Create a new project (optional)
   gcloud projects create your-project-id --name="RankForge"
   
   # Set the project
   gcloud config set project your-project-id
   
   # Enable App Engine
   gcloud app create --region=us-central1
   ```

2. **Build the application**
   ```bash
   cd rank-forge-server
   mvn clean package
   ```

3. **Deploy to App Engine**
   ```bash
   mvn appengine:deploy
   ```

4. **View your application**
   ```bash
   gcloud app browse
   ```

### Configuration

#### Update app.yaml for your project
Edit `src/main/appengine/app.yaml` and update the project ID:
```yaml
# Update the project configuration in pom.xml
<configuration>
    <projectId>your-actual-project-id</projectId>
    <version>1</version>
</configuration>
```

#### Environment Variables
You can add environment variables in `app.yaml`:
```yaml
env_variables:
  SPRING_PROFILES_ACTIVE: "gae,prod"
  DATABASE_URL: "your-database-url"
```

### Monitoring and Logs

1. **View logs**
   ```bash
   gcloud app logs tail -s default
   ```

2. **View in Cloud Console**
   - Go to Google Cloud Console
   - Navigate to App Engine → Services
   - Click on your service to view details

### Local Testing

1. **Run locally**
   ```bash
   mvn spring-boot:run
   ```

2. **Test endpoints**
   ```bash
   # API endpoint
   curl http://localhost:8080/api/rankings
   
   # Web interface
   curl http://localhost:8080/rankings
   
   # Health check
   curl http://localhost:8080/api/rankings/health
   ```

### Performance Optimization

1. **Instance Class**: For production, consider upgrading from F2 to a higher instance class in `app.yaml`
2. **Scaling**: Adjust min/max instances based on expected traffic
3. **Caching**: Consider adding Redis/Memcache for data caching

### Security Considerations

1. **HTTPS**: App Engine automatically provides HTTPS
2. **Authentication**: Add authentication for admin features if needed
3. **CORS**: Currently allows all origins - restrict in production

### Cost Optimization

1. **Free Tier**: App Engine provides generous free tier
2. **Automatic Scaling**: Configure proper scaling to avoid unnecessary costs
3. **Instance Hours**: Monitor instance usage in Cloud Console

### Troubleshooting

#### Common Issues

1. **Build Failures**
   ```bash
   # Clean and rebuild
   mvn clean install -DskipTests
   ```

2. **Memory Issues**
   ```yaml
   # Increase instance class in app.yaml
   instance_class: F4
   ```

3. **Startup Time**
   ```yaml
   # Increase app start timeout
   readiness_check:
     app_start_timeout_sec: 600
   ```

#### Health Checks
The application includes health check endpoints:
- `/api/rankings/health` - Simple health check
- Spring Boot Actuator endpoints for detailed monitoring

### Custom Domain (Optional)

1. **Map custom domain**
   ```bash
   gcloud app domain-mappings create www.yourdomain.com
   ```

2. **SSL Certificate**
   App Engine automatically manages SSL certificates for custom domains

### Backup and Recovery

1. **Source Code**: Keep source code in version control (Git)
2. **Database**: If using Cloud SQL, enable automated backups
3. **Configuration**: Store sensitive configuration in Secret Manager

This deployment setup provides a production-ready CS2 player ranking system that can scale automatically based on traffic demands.