package org.helper.elasticsearch.commands;

import okhttp3.Headers;
import okhttp3.ResponseBody;

public class RestCallReturnObj {
	private ResponseBody responseBody;
	private long callTook;
	private Headers headers;
	private int code;

	public ResponseBody getResponseBody() {
		return responseBody;
	}
	public void setResponseBody(ResponseBody responseBody) {
		this.responseBody = responseBody;
	}
	public long getCallTook() {
		return callTook;
	}
	public void setCallTook(long callTook) {
		this.callTook = callTook;
	}

	public Headers getHeaders() {
		return headers;
	}
	public void setHeaders(Headers headers) {
		this.headers = headers;
	}

	public int getCode() { return code; }
	public void setCode(int code) { this.code = code; }
}