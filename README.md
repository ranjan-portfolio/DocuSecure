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
- **Part 3** → Jenkins + Maven + Docker + EKS + ArgoCD CI/CD setup
- **Part 4** → React UI + RESTful backend rewrite
