package com.servicenow.file.upload.exception;

public abstract class FileUploadException extends Exception {

	private static final long serialVersionUID = -2500386425913177245L;

	protected FileUploadException(String message) {
		super(message);
	}

	protected FileUploadException(Exception ex) {
		super(ex);
	}

}
