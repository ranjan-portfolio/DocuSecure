package com.ranjan.cognito.DocuSecure.to;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DocumentResponseTO {

    private String fileName;
    private String fileType;
    private String filePath;
    private byte[] content;
    
}
