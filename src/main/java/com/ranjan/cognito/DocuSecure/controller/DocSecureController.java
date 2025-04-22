package com.ranjan.cognito.DocuSecure.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.nimbusds.jose.proc.SecurityContext;
import com.ranjan.cognito.DocuSecure.service.AwsService;
import com.ranjan.cognito.DocuSecure.to.DocumentDetails;
import com.ranjan.cognito.DocuSecure.to.DocumentResponseTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;


@Controller
@Getter
@Setter
public class DocSecureController {

    @Autowired
    private AwsService awsService;

    @Value("${docusecure.bucketName}")
    private String bucketName;
    
    @GetMapping("/custom-logout")
    public void customLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Invalidate Spring session
        HttpSession session=request.getSession(false);
        if(session!=null){
            session.invalidate();
        }
        //Call cognito session timeout url
        String clientId = "3j8oa6dcbpdm0c51mm5pdrtra9";
        String logoutUrl = "https://eu-west-22mxbswi4f.auth.eu-west-2.amazoncognito.com/logout"
                             + "?client_id=" + clientId
                             + "&logout_uri=" + URLEncoder.encode("http://localhost:8080/logged-out", StandardCharsets.UTF_8);
         response.sendRedirect(logoutUrl);
    
    }

    @GetMapping("/")
    public String getFileList(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        
        if (oidcUser == null) {
            return "redirect:/oauth2/authorization/cognito";
        }

        String idToken=oidcUser.getIdToken().getTokenValue();
        String username=oidcUser.getEmail();
        AwsSessionCredentials sessionCredentials=awsService.getTemporaryCredentials(idToken);
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
        List<DocumentDetails> fileList=awsService.listS3Objects(bucketName,userId, sessionCredentials);
        model.addAttribute("username", username);
        model.addAttribute("fileList", fileList);

        return "home";
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<byte[]> download(Model model, @AuthenticationPrincipal OidcUser oidcUser
                             ,@PathVariable("documentId") String  documentId) {
        
        if (oidcUser == null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/oauth2/authorization/cognito"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        String idToken=oidcUser.getIdToken().getTokenValue();
    
        AwsSessionCredentials sessionCredentials=awsService.getTemporaryCredentials(idToken);
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
        DocumentResponseTO documentResponseTO=awsService.download(documentId, bucketName, userId, sessionCredentials);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(documentResponseTO.getFileName()).build());

        return new ResponseEntity<>(documentResponseTO.getContent(), headers, HttpStatus.OK);
    }

    @PostMapping("/upload")
    public String upload(Model model,@RequestParam("file") MultipartFile multipartFile, @AuthenticationPrincipal OidcUser oidcUser) throws IOException {
        

        if (oidcUser == null) {
            return "redirect:/oauth2/authorization/cognito";
        }

        String idToken=oidcUser.getIdToken().getTokenValue();
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
    
        AwsSessionCredentials sessionCredentials=awsService.getTemporaryCredentials(idToken);
        
        awsService.upload(multipartFile, bucketName, userId, sessionCredentials);

        List<DocumentDetails> fileList=awsService.listS3Objects(bucketName,userId, sessionCredentials);
        model.addAttribute("fileList", fileList);

        return "home";
    }
    
    
}
