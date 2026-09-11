package com.codeit.modoo_playlist.moduleapi.domain.chat.exception;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class ChatException extends BaseException {
	public ChatException(ErrorCode message) {
		super(message);
	}
	public ChatException(ErrorCode errorCode, Throwable cause) {
		super(errorCode, cause);
	}
}
