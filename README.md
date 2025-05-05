# 🔐 Creating a Document Store using S3, DynamoDB, Cognito, SpringBoot, Thymeleaf — Part 2

In continuation of [Part 1](https://medium.com/...), this article details how to build a document store leveraging Cognito, S3, DynamoDB, SpringBoot and Thymeleaf. Users can securely store and download their files in **DocuSecure**.

## 👋 Introduction

This article extends Part 1 where we secured a Spring Boot application using Cognito. Here, we’ll:
- Use a **Cognito Identity Pool** to provide temporary access to AWS services (S3, DynamoDB)
- Store documents in **S3**
- Store metadata in **DynamoDB**
- Leverage Cognito for secure, key-less AWS access

## 🛠️ What We’ll Cover
- Configuring Cognito Identity Pool
- Creating IAM roles and permissions for S3/DynamoDB
- Using SpringBoot + AWS SDK to list, upload and download documents
- Explore Cognito’s built-in signup and password recovery

## 🧱 Tech Stack
- Java 17
- Spring Boot 3.x
- Spring Security (OAuth2 Client)
- Amazon Cognito (Hosted UI)
- AWS SDK for Java v2

## ⚙️ Setting Up AWS Services

### 1. Create an S3 Bucket
Use AWS Console to create a new bucket.

### 2. Create a DynamoDB Table
Table name: `Docusecure`
- Partition Key: `CustomerId`
- Sort Key: `DocumentId`
- Other fields: `fileName`, `fileType`, `filePath`

### 3–9. Create Cognito Identity Pool
- Select **Authenticated Access** and **Amazon Cognito User Pool**
- Attach IAM Role with permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cognito-identity:GetCredentialsForIdentity"
      ],
      "Resource": ["*"]
    },
    {
      "Sid": "S3Access",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::docusecure-ranjan",
        "arn:aws:s3:::docusecure-ranjan/*"
      ]
    },
    {
      "Sid": "DynamoDBAccess",
      "Effect": "Allow",
      "Action": [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Scan"
      ],
      "Resource": "arn:aws:dynamodb:eu-west-2:588578924488:table/Docusecure"
    }
  ]
}
```

## 🧩 Spring Boot Configuration

### 10. Add Dependencies

In `pom.xml`:
```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>aws-core</artifactId>
  <version>2.31.25</version>
</dependency>
<!-- Add other dependencies for oauth2, s3, dynamodb, thymeleaf, etc. -->
```

### 11. application.yml
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          cognito:
            client-id: YOUR_CLIENT_ID
            client-secret: YOUR_CLIENT_SECRET
            redirect-uri: http://localhost:8080/login/oauth2/code/cognito
            scope: [phone, openid, email]
        provider:
          cognito:
            issuerUri: https://cognito-idp.eu-west-2.amazonaws.com/YOUR_USER_POOL_ID
            user-name-attribute: username
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

aws:
  region: eu-west-2
  identityPoolId: YOUR_IDENTITY_POOL_ID
  userPoolProvider: cognito-idp.eu-west-2.amazonaws.com/YOUR_USER_POOL_ID

docusecure:
  bucketName: docusecure-ranjan
```

### 12. Get Temporary AWS Credentials from Identity Pool

### 13. Fetch Document Metadata from DynamoDB

### 14. Upload File to S3

### 15. Download File from S3

Use `documentId` to fetch metadata from DynamoDB and download file content.

## 🧪 End-to-End Test Flow

1. Launch App → Login at `localhost:8080`
2. Upload a file (PDF)
3. Download a file
4. Logout (`/custom-logout`)
5. Try `Sign Up` and `Forget Password` features

## ✨ Final Thoughts

We created a secure document store using AWS Cognito, S3, DynamoDB, and Spring Boot.

🔗 **[GitHub Repo](https://github.com/ranjan-portfolio/DocuSecure.git)**

🚀 **Coming Up Next:**
- **Part 3** → REST implementation of endpoints with Swagger with JWT validation.
- **Part 4** → Upgrading the frontend to REACT UI

----
## Part 3

# 🚀 Building and Documenting Secure REST APIs with Swagger and Spring Boot in DocuSecure

**Learn how we integrated Swagger UI and OpenAPI into DocuSecure to create secure, easily testable APIs using AWS S3, Cognito, and Spring Boot 3.**

---

## 🧩 Introduction

In our previous article, we built a secure document storage and retrieval application — **DocuSecure** — using Spring Web MVC, Thymeleaf, AWS S3, DynamoDB, and Amazon Cognito.  
Now, we're taking the next step:

- Exposing a REST API to allow users to upload, download, and retrieve documents securely
- Building a React frontend that interacts with these APIs

In this article, we'll cover:

- Creating REST APIs protected by JWT tokens (issued by Cognito)  
- Validating JWTs using Spring Security Resource Server  
- Auto-generating beautiful API documentation using Swagger UI (OpenAPI 3.0)  
- Testing secured endpoints directly from the browser  

Let’s dive in! 🏄‍♂️

---

## 🔧 Tech Stack Overview

- **Backend Framework:** Spring Boot 3.x  
- **Authentication:** Amazon Cognito (OAuth2)  
- **Cloud Storage:** AWS S3  
- **API Documentation:** Swagger (OpenAPI 3.0)

---

## 🛠️ Adding Swagger to the Project

First, we integrated Swagger using the `springdoc-openapi` library. It automatically generates OpenAPI documentation and a full Swagger UI.

### Step 1: Add the Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>
</dependency>

🔐 Securing APIs with JWT Validation
Since our application uses Cognito for authentication, we needed our backend to:

Validate JWT tokens

Authorize API access based on valid tokens

Add this dependency for OAuth2 Resource Server:
xml
Copy
Edit
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
Update your security configuration:
java
Copy
Edit
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(c -> c.disable())
        .oauth2Login(Customizer.withDefaults())  // For Thymeleaf frontend login
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))  // Validate JWT for APIs
        .authorizeHttpRequests(c -> {
            c.requestMatchers("/", "/logged-out", "/error").permitAll();
            c.requestMatchers(
                "/upload", "/download/**", "/api/**", 
                "/custom-logout", "/swagger-ui/**", "/v3/api-docs/**"
            ).authenticated();
        })
        .logout(c -> c.disable());

    return http.build();
}
✨ This enables seamless security for both frontend and backend parts of the application.

🚀 Creating the REST APIs
We created a new REST controller DocRestController.java to handle API requests.

java
Copy
Edit
@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth") // Enforce Bearer Token requirement
@Tag(name = "DocuSecure", description = "API for Secure Document Storage")
public class DocRestController {
    ...
}
📂 Example 1: Fetch List of Uploaded Files
java
Copy
Edit
@GetMapping("/")
@Operation(summary = "Get all files", description = "Retrieve all uploaded documents.")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successful retrieval"),
    @ApiResponse(responseCode = "500", description = "Error retrieving documents")
})
public ResponseEntity<List<FileResponse>> getFileList(@AuthenticationPrincipal Jwt jwt) {
    try {
        String idToken = jwt.getTokenValue();
        AwsSessionCredentials sessionCredentials = awsService.getTemporaryCredentials(idToken, controllerType);
        String userId = jwt.getClaimAsString("cognito:username");

        List<DocumentDetails> fileList = awsService.listS3Objects(bucketName, userId, sessionCredentials);

        List<FileResponse> fileResponses = fileList.stream()
            .map(file -> new FileResponse(file.getDocumentId(), file.getDocumentName()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(fileResponses);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
📤 Example 2: Upload New Files
java
Copy
Edit
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Operation(summary = "Upload file", description = "Upload one or more documents.")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
    @ApiResponse(responseCode = "500", description = "Error uploading file")
})
public ResponseEntity<List<FileResponse>> upload(
    @Parameter(description = "Files to be uploaded", required = true)
    @RequestParam("file") List<MultipartFile> multipartFiles,
    @AuthenticationPrincipal Jwt jwt
) throws IOException {
    ...
}
✅ Notice that we accept multiple files and validate each upload securely with the JWT token.

🖥️ Accessing Swagger UI
Once your app is running locally, visit:

bash
Copy
Edit
http://localhost:8080/swagger-ui.html
You'll see an auto-generated Swagger page listing all available endpoints.

🛡️ Authorizing Swagger API Calls
Click the Authorize button at the top-right.

Enter your Bearer Token (ID token from Cognito login).

Click Authorize → Now you can test authenticated endpoints!

🔥 A Quick Demo of Endpoints
GET /api/ — Fetch list of files uploaded by the logged-in user

POST /api/upload — Upload multiple documents via Swagger UI

GET /api/download/{fileId} — Download a document by ID

✅ Swagger even generates handy buttons like "Try it Out" and "Execute" to simplify testing!

---
## For Part 4 check https://github.com/ranjan-portfolio/docusecure-react



