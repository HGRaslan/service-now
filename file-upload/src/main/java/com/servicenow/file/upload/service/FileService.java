package com.servicenow.file.upload.service;

import org.springframework.web.multipart.MultipartFile;

import com.servicenow.file.upload.exception.FileUploadException;

public interface FileService {

	void saveFile(MultipartFile file, int retentionPeriodInHours) throws FileUploadException;
}
