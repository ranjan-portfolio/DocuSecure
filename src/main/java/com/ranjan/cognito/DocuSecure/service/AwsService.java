package com.ranjan.cognito.DocuSecure.service;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ranjan.cognito.DocuSecure.to.DocumentDetails;
import com.ranjan.cognito.DocuSecure.to.DocumentResponseTO;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.Credentials;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class AwsService {

    @Value("${aws.web.identityPoolId}")
    private String webIdentityPoolId;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.web.userPoolProvider}")
    private String webUserPoolProvider;

    @Value("${aws.rest.identityPoolId}")
    private String restIdentityPoolId;

    @Value("${aws.rest.userPoolProvider}")
    private String restUserPoolProvider;

    public AwsSessionCredentials getTemporaryCredentials(String idToken,String controller) {

        String identityPoolId = controller.equals("WEB") ? webIdentityPoolId : restIdentityPoolId;
        String provider = controller.equals("WEB") ? webUserPoolProvider : restUserPoolProvider;

        CognitoIdentityClient identityClient = CognitoIdentityClient.builder()
            .region(Region.of(region))
            .build();

        Map<String, String> logins = Map.of(provider, idToken);

        String identityId = identityClient.getId(GetIdRequest.builder()
                .identityPoolId(identityPoolId)
                .logins(logins)
                .build()).identityId();

        Credentials credentials=identityClient.getCredentialsForIdentity(builder -> builder
                .identityId(identityId)
                .logins(logins))
            .credentials();

        return AwsSessionCredentials.create(
                credentials.accessKeyId(),
                credentials.secretKey(),
                credentials.sessionToken()
        );
    }

    public List<DocumentDetails> listS3Objects(String bucketName,String userId, AwsSessionCredentials creds) {

        DynamoDbClient dynamoClient=DynamoDbClient.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(creds))
        .build();

        Map<String,AttributeValue> item=new HashMap<>();
        item.put("CustomerId",AttributeValue.builder().s(userId).build());

        QueryRequest request=QueryRequest.builder()
                                         .tableName("Docusecure")
                                         .keyConditionExpression("CustomerId= :cid")
                                         .expressionAttributeValues(Map.of(":cid",AttributeValue.fromS(userId)))
                                         .build();
        QueryResponse response= dynamoClient.query(request);

        List<Map<String, AttributeValue>> items = response.items();
        List<DocumentDetails> documentDetails=items.stream().map(s->{
            DocumentDetails document=new DocumentDetails();
            document.setDocumentId(s.get("DocumentId").s());
            document.setDocumentName(s.get("Filename").s());
            return document;
        }).collect(Collectors.toList());

        return documentDetails;
    }

    public String upload(MultipartFile file,String bucketName,String userId, AwsSessionCredentials creds) throws IOException {
        
       
        S3Client s3 = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
       
        String key=userId+"/"+file.getOriginalFilename();
        PutObjectRequest putObjectRequest=PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
       
        s3.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        //Generate UUID and save file details with userID in DynamoDB

        UUID uuid=UUID.randomUUID();

        DynamoDbClient dynamoClient=DynamoDbClient.builder()
                                    .region(Region.of(region))
                                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                                    .build();

        Map<String,AttributeValue> item=new HashMap<>();
        item.put("CustomerId",AttributeValue.builder().s(userId).build());
        item.put("DocumentId",AttributeValue.builder().s(uuid.toString()).build());
        item.put("Filename",AttributeValue.builder().s(file.getOriginalFilename()).build());
        item.put("FilePath",AttributeValue.builder().s(key).build());
        item.put("FileType",AttributeValue.builder().s(file.getContentType()).build());

        PutItemRequest request=PutItemRequest.builder()
                                            .tableName("Docusecure")
                                            .item(item)
                                            .build();
        dynamoClient.putItem(request);

        return uuid.toString();
    }

    public DocumentResponseTO download(String documentId,String bucketName,String userId, AwsSessionCredentials creds){
        
        DocumentResponseTO documentResponseTO=new DocumentResponseTO();
        
        DynamoDbClient dynamoClient=DynamoDbClient.builder()
                                    .region(Region.of(region))
                                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                                    .build();

        Map<String,AttributeValue> queryKeys=new HashMap<>();
        queryKeys.put("CustomerId",AttributeValue.builder().s(userId).build());
        queryKeys.put("DocumentId",AttributeValue.builder().s(documentId).build());

        GetItemRequest request=GetItemRequest.builder().tableName("Docusecure").key(queryKeys).build();
        GetItemResponse response= dynamoClient.getItem(request);
        Map<String, AttributeValue> item=response.item();
        if(item==null){
            //throw exception
        }else{
            documentResponseTO.setFileName(item.get("Filename").s());
            documentResponseTO.setFileType(item.get("FileType").s());
            documentResponseTO.setFilePath(item.get("FilePath").s());
        }

        S3Client s3 = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(creds))
        .build();

        if(documentResponseTO.getFilePath()!=null){
            String key=documentResponseTO.getFilePath();
            GetObjectRequest objectRequest= GetObjectRequest.builder().bucket(bucketName).key(key).build();
            ResponseBytes<GetObjectResponse> content= s3.getObjectAsBytes(objectRequest);
            if(content==null){
                throw new RuntimeException("File not found");
            }
            documentResponseTO.setContent(content.asByteArray());
        }

        return documentResponseTO;
    }
}

