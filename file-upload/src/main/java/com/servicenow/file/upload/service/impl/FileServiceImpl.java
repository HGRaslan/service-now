package com.servicenow.file.upload.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.servicenow.file.upload.config.FileServiceConfig;
import com.servicenow.file.upload.constants.Constants;
import com.servicenow.file.upload.exception.FileAlreadyExistsException;
import com.servicenow.file.upload.exception.FileUploadException;
import com.servicenow.file.upload.exception.FileUploadFailedException;
import com.servicenow.file.upload.service.FileService;

@Service
public class FileServiceImpl implements FileService {

	private static final Logger LOGGER = Logger.getLogger("FileServiceImpl");

	private FileServiceConfig configs;

	public FileServiceImpl(FileServiceConfig configs) {
		this.configs = configs;
	}

	@Override
	public void saveFile(MultipartFile file, int retentionPeriodInHours) throws FileUploadException {
		try {
			File dir = createDestinationDirIfNotExists();

			File destinationFile = transferFile(file, dir);

			setFileExpirationAttributes(destinationFile.toPath(), retentionPeriodInHours);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed to upload file [" + file.getOriginalFilename() + "]", ex);
			rollbackIfNeeded(file.getOriginalFilename());
			throw new FileUploadFailedException(ex);
		}
	}

	private void rollbackIfNeeded(String originalFilename) {
		try {
			File destinationFile = new File(configs.getFileSavePath(), originalFilename);

			if (destinationFile.exists()) {
				Files.delete(destinationFile.toPath());
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to clean up file [" + originalFilename + "]", e);
		}
	}

	private void setFileExpirationAttributes(Path destinationFilePath, int retentionPeriodInHours) throws IOException {
		long fileExpirationTime = System.currentTimeMillis() + Duration.ofHours(retentionPeriodInHours).toMillis();
		Files.setAttribute(destinationFilePath, Constants.FILE_ATT_EXPIRATION_TIME, String.valueOf(fileExpirationTime).getBytes());
	}

	private File transferFile(MultipartFile file, File destinationDir) throws IOException, FileAlreadyExistsException {
		File destinationFile = new File(destinationDir, file.getOriginalFilename());

		if (destinationFile.exists()) {
			throw new FileAlreadyExistsException("File [" + file.getOriginalFilename() + "] already exists.");
		}

		file.transferTo(destinationFile);

		return destinationFile;
	}

	private File createDestinationDirIfNotExists() {
		File dir = new File(configs.getFileSavePath());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

}
