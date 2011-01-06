/*
 * Copyright 2010-2011 Andrea De Pasquale
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;

/**
 * Abstract class for web message senders.
 * 
 * @author Andrea De Pasquale
 */
public abstract class WebSender {

    static final String TAG = "WebSender";
    
    protected HttpClient httpClient;
    protected HttpContext httpContext;
    protected WebSenderCookieStore cookieStore;
    static final String COOKIES = "cookies";
    
    protected Context context;
    protected WebSenderHelper helper;

    /**
     * Default constructor that initializes HTTP connections.
     */
    public WebSender(Context context) {
	this.context = context;
	httpClient = new DefaultHttpClient();
	httpContext = new BasicHttpContext();
	
	loadCookies(); // from cookie file
	httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    /**
     * Call this method to let send() run quicker 
     */
    public abstract void preSend() throws Exception;

    /**
     * Call this method to send someone a text message.
     * 
     * @param sms Text message to be sent
     * @param captcha Captcha text manually decoded by the user.
     */
    public abstract boolean send(WebSMS sms, String captcha) throws Exception;

    /**
     * Retrieve old saved cookies or get a new cookie store
     * @return a cookie store
     */
    protected boolean loadCookies() {
	try {
	    
	    FileInputStream fileInput = context.openFileInput(COOKIES);
	    ObjectInputStream objectInput = new ObjectInputStream(fileInput);
	    cookieStore = (WebSenderCookieStore) objectInput.readObject();
	    objectInput.close();
	    fileInput.close();
	    return true;
	    
	} catch (Exception e) {
	    new WebSenderCookieStore();
	    return false; 
	}
    }
    
    /**
     * Save current cookies for future reuse.
     */
    protected boolean saveCookies() {
	try {
	    
	    FileOutputStream fileOutput = 
		context.openFileOutput(COOKIES, Context.MODE_PRIVATE);
	    ObjectOutputStream objectOutput = 
		new ObjectOutputStream(fileOutput);
	    objectOutput.writeObject(cookieStore);
	    objectOutput.close();
	    fileOutput.close();
	    return true;
	    
	} catch (Exception e) {
	    return false;
	}
    }
    
    protected byte[] captchaArray; // XXX remove

    public byte[] getCaptchaArray() {
	return captchaArray; // XXX remove
    }

}
