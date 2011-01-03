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

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Abstract class for web message senders.
 * 
 * @author Andrea De Pasquale
 */
public abstract class WebSender {

    static final String TAG = "WebSender";
    
    protected HttpClient httpClient;
    protected HttpContext httpContext;
    
    protected Context context;
    protected SharedPreferences sharedPreferences;
    protected SMSDatabase smsDatabase;

    /**
     * Default constructor that initializes HTTP connections.
     */
    public WebSender(Context context) {
	httpClient = new DefaultHttpClient();
	httpContext = new BasicHttpContext();
	
	this.context = context;
	sharedPreferences = 
	    PreferenceManager.getDefaultSharedPreferences(context);
	smsDatabase = new SMSDatabase(context);
    }

    /**
     * Call this method to send someone a text message.
     * 
     * @param sms Text message to be sent
     * @param captcha Captcha text manually decoded by the user.
     */
    public abstract boolean send(SMS sms, String captcha) throws Exception;

    /**
     * Call this method to retrieve information based on current message length.
     * 
     * @param length Length of the text message.
     * @return an <code>int[]</code>, containing how many messages will be sent
     *         (at index 0) and how many characters are remaining (at index 1).
     */
    public abstract int[] calcLength(int length);

    /**
     * Call this method to retrieve information about message count.
     * 
     * @return an <code>int[]</code>, containing how many messages have been
     * 	       sent today (at index 0) and how many messages can be sent in a
     *         single day (at index 1).
     */
    public abstract int[] getCount();

    protected byte[] captchaArray;

    public byte[] getCaptchaArray() {
	return captchaArray;
    }

}
