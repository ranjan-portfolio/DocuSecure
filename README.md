🔐 Building a Secure Document Store with AWS (S3, DynamoDB, Cognito) & Spring Boot — Part 2

In continuation of part 1 , this article details how to build a document store leveraging Cognito, S3, DynamoDB, SpringBoot and Thymeleaf. User can store and download their files in DocuSecure securely.

👋 Introduction

A detailed step-by-step of the document store is discussed in below medium blog

https://medium.com/@ranjanabhabhattacharya/building-a-secure-document-store-with-aws-s3-dynamodb-cognito-spring-boot-part-2-8581d61766a6

Continuing from Part 1, where we secured a Spring Boot application using Amazon Cognito, this article demonstrates how to build a secure document store using AWS S3, DynamoDB, Spring Boot, and Thymeleaf. In this project, we leverage Cognito Identity Pools to provide temporary access to AWS resources without managing credentials manually.

🛠️ What We’ll Cover
* How to configure a Cognito Identity Pool.
* Setting up IAM roles and integrating them with Cognito for S3 and DynamoDB access.
* Using Spring Boot and the AWS SDK to list, upload, and download files securely.
* Exploring Cognito's built-in signup and password reset features.

🧱 Tech Stack
* Java 17
* Spring Boot 3.x
* Spring Security (OAuth2 Client)
* Amazon Cognito (Hosted UI)
* AWS SDK for Java v2

⚙️ Setting Up Cognito Identity Pool,DynamoDb and S3 in Hosted AWS

1. Create an S3 bucket by navigating to the S3 service in AWS and clicking on "Create bucket."

   
2. Head over to DynamoDB UI in AWS and create a table, We have used Partition Key as ‘CustomerId’ and ‘Sort Key’ as DocumentId. Other attributes that has been considered are FileName, FileType and FilePath

￼
3. Select ‘Authenticated access’ in User access and ‘Amazon Cognito user pool’ in ‘Authenticated identity source’ of Authentication section and press Next
￼

4. Create a new IAM role, Here since we have already created the role for application development so I have selected ‘Use an existing IAM role’
￼

5. Following IAM permissions needs to be added in the new IAM role
￼

IAM permissions


1. ✅ First permission allows Cognito to retrieve temporary credentials {
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cognito-identity:GetCredentialsForIdentity"
            ],
            "Resource": [
                "*"
            ]
        }
    ]
}

2. ✅ Second block grants specific access to S3 and DynamoDB resources.{
	"Version": "2012-10-17",
	"Statement": [
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

6. In Trust relationship, please ensure if Cognito-identity.amazonaws.com:aud is your identity pool id

7. Select your User pool and App Client id that we have created in Part 1 of the tutorial and press Next
￼
8. Give identity pool name and press Next
￼
9. Review and click on ‘Create Identity pool’
￼
10. 🧩 Spring Boot Configuration

Create a spring application. I have created a blank project using spring initialiser.
Add following dependencies in pom.xml

Pom.xml

<dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>aws-core</artifactId>
            <version>2.31.25</version>
        </dependency>

            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-oauth2-client</artifactId>
            </dependency>
            <!-- https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk -->
            
            <!-- https://mvnrepository.com/artifact/software.amazon.awssdk/cognitoidentityprovider -->
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>cognitoidentity</artifactId>
                <version>2.31.25</version>
            </dependency>
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>s3</artifactId>
                <version>2.31.25</version>
            </dependency>
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>dynamodb</artifactId>
                <version>2.31.25</version>
            </dependency>
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>auth</artifactId>
                <version>2.31.25</version>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-security</artifactId>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-thymeleaf</artifactId>
            </dependency>
            <dependency>
                <groupId>org.thymeleaf.extras</groupId>
                <artifactId>thymeleaf-extras-springsecurity6</artifactId>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-devtools</artifactId>
                <scope>runtime</scope>
                <optional>true</optional>
            </dependency>
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <optional>true</optional>
            </dependency>
    </dependencies>



11. application.yml
spring:
    security:
        oauth2:
            client:
                registration:
                    cognito:
                        client-id: 3j8oa6dcbpdm0c51mm5pdrtra9
                        client-secret: f45d5463upgid5i4hsv5d5vqc3td9nj1mnuitfeio5uojqe9mg2
                        scope:
                        - phone
                        - openid
                        - email
                        # Spring Security by default uses a redirect-uri in the format: {baseUrl}/login/oauth2/code/{registrationId}
                        # For example: http://localhost:8080/login/oauth2/code/cognito
                        # See more: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html#oauth2login-sample-redirect-uri
                        redirect-uri: http://localhost:8080/login/oauth2/code/cognito
                provider:
                    cognito:
                        issuerUri: https://cognito-idp.eu-west-2.amazonaws.com/eu-west-2_2mxbsWi4F
                        user-name-attribute: username
    servlet:
        multipart:
            max-file-size: 20MB
            max-request-size: 20MB

aws:
    region: eu-west-2
    identityPoolId: eu-west-2:f2924214-28b1-48a1-adec-3e74b213a410
    userPoolProvider: cognito-idp.eu-west-2.amazonaws.com/eu-west-2_2mxbsWi4F

docusecure:
        bucketName: docusecure-ranjan


  

￼
✨ Final Thoughts

We’ve successfully built a secure document storage application using Cognito, S3, DynamoDB, and Spring Boot. In the next part, we'll integrate CI/CD tools like Jenkins, Maven, Docker, EKS, and ArgoCD. Part 4 will feature a revamped UI using React and a RESTful backend.
