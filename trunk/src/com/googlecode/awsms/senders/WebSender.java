/*
 * Copyright 2010 Andrea De Pasquale
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.awsms.senders;

import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * Abstract class for web message senders.
 * 
 * @author Andrea De Pasquale
 */
public abstract class WebSender {

	protected HttpClient httpClient;
	protected HttpContext httpContext;
	
	/**
	 * Default constructor that initializes HTTP connections.
	 */
	public WebSender() {
		httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter("http.protocol.allow-circular-redirects", true);
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
	}
	
	public enum Result { 
		SETTINGS_INVALID, RECEIVER_INVALID, MESSAGE_INVALID, 
		WEBSITE_UNAVAILABLE, OUT_OF_MESSAGES, NEED_CAPTCHA, 
		UNKNOWN_ERROR, MESSAGE_SENT
	};

	/**
	 * Call this method to send someone a text message.
	 * @param username Username to enter private website area.
	 * @param password Password to enter private website area.
	 * @param receiver Phone number of the message addressee.
	 * @param message Text message to be sent to the receiver.
	 * @param captcha Captcha text manually decoded by the user.
	 * @return <code>enum</code> representing what happened. 
	 */
	public abstract Result send(String username, String password, String receiver, String message, String captcha);

	/**
	 * Call this method to retrieve information based on current message length.
	 * @param messageLength Length of the text message.
	 * @return an <code>int[]</code>, containing how many messages will be sent 
	 * (at index 0) and how many characters are remaining (at index 1).
	 */
	public abstract int[] getInformation(int messageLength);
	
	/**
	 * Strips international prefix from a phone number.
	 * @param prefix Country code (e.g. "+39" for Italy).
	 * @param number Phone number to be stripped.
	 * @return The number without the prefix.
	 */
	protected String stripPrefix(String prefix, String number) {
		if (number.length() > prefix.length() &&
				number.substring(0, prefix.length()).equals(prefix)) {
			return number.substring(prefix.length());
		}
		
		return number;
	}
	
	protected byte[] captchaArray;
	
	public byte[] getCaptchaArray() {
		return captchaArray;
	}
	
}
