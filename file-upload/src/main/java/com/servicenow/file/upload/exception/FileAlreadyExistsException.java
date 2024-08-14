package com.servicenow.file.upload.exception;

public class FileAlreadyExistsException extends FileUploadException {

	private static final long serialVersionUID = -126565629523492885L;

	public FileAlreadyExistsException(String message) {
		super(message);
	}
}
