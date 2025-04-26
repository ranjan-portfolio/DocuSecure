package com.ranjan.cognito.DocuSecure.restcontroller;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ranjan.cognito.DocuSecure.response.FileResponse;
import com.ranjan.cognito.DocuSecure.service.AwsService;
import com.ranjan.cognito.DocuSecure.to.DocumentDetails;
import com.ranjan.cognito.DocuSecure.to.DocumentResponseTO;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;


@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@Getter
@Setter
@Tag(name = "DocuSecure", description = "API for secure Document Storage")
public class DocRestController {

    @Autowired
    private AwsService awsService;
    
    @Value("${docusecure.bucketName}")
    private String bucketName;

    private String controllerType="REST";

    @GetMapping("/")
    @Operation(summary = "Get all files", description = "Get all files")
    @ApiResponses(value={
        @ApiResponse(responseCode = "200",description = "Get all files"),
        @ApiResponse(responseCode = "500",description = "Problem encountered while fetching files")
    })
    public ResponseEntity<List<FileResponse>> getFileList(@AuthenticationPrincipal Jwt jwt) {
        List<FileResponse> file=null;
            try{
                String idToken=jwt.getTokenValue();
                AwsSessionCredentials sessionCredentials=awsService.getTemporaryCredentials(idToken,controllerType);
                String userId=jwt.getClaimAsString("cognito:username");
                List<DocumentDetails> fileList=awsService.listS3Objects(bucketName,userId, sessionCredentials);
                file=fileList.stream().map((fileDetails)->{
                    FileResponse fileResponse=new FileResponse();
                    fileResponse.setFileId(fileDetails.getDocumentId());
                    fileResponse.setName(fileDetails.getDocumentName());
                    return fileResponse;
                }).collect(Collectors.toList());
            return ResponseEntity.ok().body(file);
            } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        
    }

    @GetMapping("/download/{documentId}")
    @Operation(summary = "Download file", description = "Download file")
    @ApiResponses(value={
        @ApiResponse(responseCode = "200", description = "Download file"),
        @ApiResponse(responseCode = "500", description = "Problem encountered while downloading file")
    })
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt
                             ,@PathVariable("documentId") String  documentId) {
        try{
            String idToken=jwt.getTokenValue();
            AwsSessionCredentials sessionCredentials=awsService.getTemporaryCredentials(idToken,controllerType);
            String userId=jwt.getClaimAsString("cognito:username");
            DocumentResponseTO documentResponseTO=awsService.download(documentId, bucketName, userId, sessionCredentials);
    
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename(documentResponseTO.getFileName()).build());
    
            return new ResponseEntity<>(documentResponseTO.getContent(), headers, HttpStatus.OK);

        }catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
       
    }

    @PostMapping(value="/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", 
            description = "Upload file")
    @ApiResponses(value={
        @ApiResponse(responseCode = "200", description = "Upload file"),
        @ApiResponse(responseCode = "500", description = "Problem encountered while uploading file")
    })
    public ResponseEntity<List<FileResponse>> upload(@Parameter(description = "The file to be uploaded", required = true)
                                                    @RequestParam("file") List<MultipartFile> multipartFile, 
                                                    @AuthenticationPrincipal Jwt jwt
                                                    ) throws IOException {
        
        List<FileResponse> file=null;
        try{
            String idToken=jwt.getTokenValue();
            AwsSessionCredentials sessionCredentials=awsService.getTemporaryCredentials(idToken,controllerType);
            String userId=jwt.getClaimAsString("cognito:username");
            for(MultipartFile uploadFile:multipartFile){
                awsService.upload(uploadFile, bucketName, userId, sessionCredentials);
            }
            List<DocumentDetails> fileList=awsService.listS3Objects(bucketName,userId, sessionCredentials);
            file=fileList.stream().map((fileDetails)->{
                FileResponse fileResponse=new FileResponse();
                fileResponse.setFileId(fileDetails.getDocumentId());
                fileResponse.setName(fileDetails.getDocumentName());
                return fileResponse;
            }).collect(Collectors.toList());

            return ResponseEntity.ok().body(file);

        }catch(IOException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }   
        catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        
    }
    
}
