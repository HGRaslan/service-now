package com.servicenow.file.upload.exception;

public class FileUploadFailedException extends FileUploadException{

	private static final long serialVersionUID = 2293939332337198137L;

	public FileUploadFailedException(String message) {
		super(message);
	}

	public FileUploadFailedException(Exception ex) {
		super(ex);
	}
}
