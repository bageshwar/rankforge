# Testing Guide - Verifying S3 Configuration

This guide helps you verify that your S3 configuration is wired up correctly.

## Quick Test Methods

### Method 1: Test S3 Connection via Application Startup

1. **Start the application with local profile:**
   ```bash
   cd rank-forge/rank-forge-server
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **Check the logs for S3 client initialization:**
   Look for one of these messages:
   - `"Initialized S3 client with provided credentials for region: ap-south-1"` ✅
   - `"Initialized S3 client with default credential chain for region: ap-south-1"` ✅
   - If you see errors about credentials, the configuration isn't loading correctly ❌

### Method 2: Test via Health Endpoint

1. **Start the application:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **Test health endpoint (no auth required):**
   ```bash
   curl http://localhost:8080/api/pipeline/health
   ```
   Expected: `Pipeline API is healthy`

### Method 3: Test S3 Download via Pipeline Endpoint

1. **Start the application:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **Test with a real S3 file path (single line):**
   ```bash
   curl -X POST http://localhost:8080/api/pipeline/process -H "Content-Type: application/json" -H 'X-API-Key: YOUR_API_KEY_HERE' -d '{"s3Path": "s3://cs2serverdata/cs2_log_2026-01-07.json"}'
   ```
   
   **Note:** 
   - Replace `YOUR_API_KEY_HERE` with the actual API key from `application-local.properties` (property: `rankforge.api.key`)
   - Use single quotes around the API key header value to prevent shell history expansion if your key contains special characters like `!`
   
   **Or multi-line format:**
   ```bash
   curl -X POST http://localhost:8080/api/pipeline/process \
     -H "Content-Type: application/json" \
     -H 'X-API-Key: YOUR_API_KEY_HERE' \
     -d '{"s3Path": "s3://cs2serverdata/cs2_log_2026-01-07.json"}'
   ```
   
   **Alternative:** Escape special characters if using double quotes:
   ```bash
   curl -X POST http://localhost:8080/api/pipeline/process -H "Content-Type: application/json" -H "X-API-Key: YOUR_API_KEY_HERE" -d '{"s3Path": "s3://cs2serverdata/cs2_log_2026-01-07.json"}'
   ```

3. **Expected response:**
   ```json
   {
     "jobId": "uuid-here",
     "status": "processing",
     "message": "Log processing started successfully"
   }
   ```

4. **Check application logs** for:
   - `"Downloaded X lines from S3 for job ..."` ✅ Success
   - `"S3 error"` or `"File not found"` ❌ Check credentials/path

### Method 4: Run Integration Test

1. **Update the test file** `S3ServiceIntegrationTest.java`:
   - Replace `"s3://your-bucket-name/test-file.json"` with an actual S3 path

2. **Run the test:**
   ```bash
   mvn test -Dtest=S3ServiceIntegrationTest -Dspring.profiles.active=local
   ```

## Verification Checklist

- [ ] Application starts without errors
- [ ] Logs show S3 client initialized with your region (ap-south-1)
- [ ] Health endpoint returns 200 OK
- [ ] Pipeline endpoint accepts requests with valid API key
- [ ] S3 file download succeeds (check logs)
- [ ] No "Access Denied" or "Invalid credentials" errors

## Common Issues

### Issue: "API key not configured"
**Solution:** Add `rankforge.api.key=your-key` to `application-local.properties`

### Issue: "S3 error" or "Access Denied"
**Possible causes:**
1. Invalid access key or secret key
2. IAM permissions missing (need `s3:GetObject` permission)
3. Wrong region (should be `ap-south-1` for your config)
4. Bucket doesn't exist or path is incorrect

**Check:**
```bash
# Test S3 access directly
aws s3 ls s3://your-bucket-name/ --region ap-south-1
```

### Issue: "File not found in S3"
**Solution:** Verify:
1. The S3 path is correct
2. The file exists in the bucket
3. You have read permissions for that object

## Testing with a Sample File

1. **Upload a test log file to S3:**
   ```bash
   aws s3 cp test-log.json s3://your-bucket-name/test/test-log.json --region ap-south-1
   ```

2. **Process it via the API:**
   ```bash
   curl -X POST http://localhost:8080/api/pipeline/process \
     -H "Content-Type: application/json" \
     -H "X-API-Key: your_api_key" \
     -d '{"s3Path": "s3://your-bucket-name/test/test-log.json"}'
   ```

3. **Monitor logs** for processing status

## Debug Mode

To see more detailed logs, add to `application-local.properties`:
```properties
logging.level.com.rankforge.server.service.S3Service=DEBUG
logging.level.com.rankforge.server.service.LogProcessingService=DEBUG
```
