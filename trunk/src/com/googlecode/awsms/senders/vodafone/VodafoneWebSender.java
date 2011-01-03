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

package com.googlecode.awsms.senders.vodafone;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import android.content.Context;
import android.util.Log;

import com.googlecode.awsms.R;
import com.googlecode.awsms.senders.WebSMS;
import com.googlecode.awsms.senders.WebSender;
import com.googlecode.awsms.senders.captcha.Base64;

/**
 * <code>WebSender</code> implementation for www.vodafone.it web site
 * 
 * @author Andrea De Pasquale
 */
public class VodafoneWebSender extends WebSender {
    
    static final String TAG = "VodafoneWebSender";

    // URLs used by the widget to login/logout, send SMS, etc.
    // https://widget.vodafone.it/190/trilogy/jsp/login.do
    // https://widget.vodafone.it/190/trilogy/jsp/logout.do
    // https://widget.vodafone.it/190/trilogy/jsp/swapSim.do
    // https://widget.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp
    // https://widget.vodafone.it/190/fsms/precheck.do?channel=VODAFONE_DW
    // https://widget.vodafone.it/190/fsms/prepare.do?channel=VODAFONE_DW
    // https://widget.vodafone.it/190/fsms/send.do?channel=VODAFONE_DW
    // https://widget.vodafone.it/190/fast/mx/CreditoResiduoPush.do?hpfdtpri=y
    // https://widget.vodafone.it/190/jone/mx/SaldoPuntiPush.do?hpfdtpri=y
    // https://widget.vodafone.it/190/ebwe/mx/PushInfoconto.do?hpfdtpri=y

    // XMLs containing information about the widget itself
    // http://demos.vodafone.it/dw/getDWConfiguration.xml
    // http://demos.vodafone.it/dw/updates.xml
    
    public VodafoneWebSender(Context context) {
	super(context);

	httpClient.getParams().setParameter(
		"http.protocol.allow-circular-redirects", true);
	httpClient.getParams().setParameter("http.useragent", "Vodafone_DW");
	httpContext.setAttribute(ClientContext.COOKIE_STORE,
		new BasicCookieStore());
	
	helper = new VodafoneWebSenderHelper(context);

	try {
	    // to allow self-signed certificate to be accepted
	    SSLSocketFactory socketFactory = new SSLSocketFactory(null);
	    Scheme sch = new Scheme("https", socketFactory, 443);
	    httpClient.getConnectionManager().getSchemeRegistry().register(sch);
	} catch (Exception e) {
	    Log.e(TAG, "SSLSocketFactory exception: " + e.getMessage());
	}
	
	try {
	    doLogin();
	} catch (Exception e) {
	    // don't worry, we will catch it during send()
	}
    }

    public boolean send(WebSMS sms, String captcha) throws Exception {
	// TODO support multiple receivers
	String receiver = sms.getReceivers()[0];
	String message = sms.getMessage();

	if (captcha.equals("")) {
	    if (!isLoggedIn()) doLogin();
	    doPrecheck();
	    if (!doPrepare(receiver, message))
		return false; // need CAPTCHA
	}

	if (!doSend(receiver, message, captcha))
	    return false; // still need CAPTCHA
	helper.addCount(message.length());
	return true;
    }

    /**
     * Check if the user is logged in to www.vodafone.it
     * 
     * @return true if the user is logged in, false otherwise
     * @throws Exception
     */
    private boolean isLoggedIn() throws Exception {
	Document document;

	try {
	    HttpGet request = new HttpGet(
		    "https://widget.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp");
	    HttpResponse response = httpClient.execute(request, httpContext);
	    document = new SAXBuilder()
		    .build(response.getEntity().getContent());
	    response.getEntity().consumeContent();
	} catch (Exception e) {
	    throw new Exception(
		    context.getString(R.string.WebSenderNetworkError));
	}

	Element root = document.getRootElement();
	Element child = root.getChild("logged-in");
	return child.getValue().equals("true");
    }

    /**
     * Login to www.vodafone.it website
     * 
     * @throws Exception
     */
    private void doLogin() throws Exception {
	try {
	    HttpPost request = new HttpPost(
		    "https://widget.vodafone.it/190/trilogy/jsp/login.do");
	    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
	    requestData.add(
		    new BasicNameValuePair("username", helper.getUsername()));
	    requestData.add(
		    new BasicNameValuePair("password", helper.getPassword()));
	    request.setEntity(
		    new UrlEncodedFormEntity(requestData, HTTP.ISO_8859_1));
	    HttpResponse response = httpClient.execute(request, httpContext);
	    response.getEntity().consumeContent();
	} catch (Exception e) {
	    throw new Exception(
		    context.getString(R.string.WebSenderNetworkError));
	}

	if (!isLoggedIn()) {
	    throw new Exception(
		    context.getString(R.string.WebSenderSettingsInvalid));
	}
    }

    private void parseError(int error) throws Exception {
	switch (error) {
	case 107:
	    throw new Exception(
		    context.getString(R.string.WebSenderLimitReached));
	
	case 113:
	    throw new Exception(
		    context.getString(R.string.WebSenderReceiverNotAllowed));
	    
	case 104: // ?
	case 109: // Messaggio vuoto
	default:
	    throw new Exception(
		    context.getString(R.string.WebSenderUnknownError));
	}
    }

    /**
     * Page to be visited before sending a message
     * 
     * @throws Exception
     */
    private void doPrecheck() throws Exception {
	Document document;

	try {
	    HttpGet request = new HttpGet(
		    "https://widget.vodafone.it/190/fsms/precheck.do?channel=VODAFONE_DW");
	    HttpResponse response = httpClient.execute(request, httpContext);

	    Reader reader = new BufferedReader(new InputStreamReader(response
		    .getEntity().getContent()));
	    
	    if (helper.getCount() < helper.getLimit()) {
		reader.skip(13); // this trick solves XML header problem
	    }

	    document = new SAXBuilder().build(reader);
	    response.getEntity().consumeContent();

	} catch (Exception e) {
	    e.printStackTrace();
	    throw new Exception(
		    context.getString(R.string.WebSenderNetworkError));
	}

	Element root = document.getRootElement();
	@SuppressWarnings("unchecked")
	List<Element> children = root.getChildren("e");
	Log.d("VodafoneIT", "doPrecheck()");
	int status = 0, errorcode = 0;
	for (Element child : children) {
	    Log.d("VodafoneIT", child.getAttributeValue("n"));
	    if (child.getAttributeValue("v") != null)
		Log.d("VodafoneIT", child.getAttributeValue("v"));
	    if (child.getValue() != null)
		Log.d("VodafoneIT", child.getValue());
	    if (child.getAttributeValue("n").equals("STATUS"))
		status = Integer.parseInt(child.getAttributeValue("v"));
	    if (child.getAttributeValue("n").equals("ERRORCODE"))
		errorcode = Integer.parseInt(child.getAttributeValue("v"));
	}

	Log.d("VodafoneIT", "status code: " + status);
	Log.d("VodafoneIT", "error code: " + errorcode);
	if (status != 1)
	    parseError(errorcode);
    }

    /**
     * Prepare the message to be sent.
     * 
     * @param receiver
     * @param message
     * @throws Exception
     * @returns false if CAPTCHA present
     */
    private boolean doPrepare(String receiver, String message) throws Exception {
	Document document;

	try {
	    HttpPost request = new HttpPost(
		    "https://widget.vodafone.it/190/fsms/prepare.do?channel=VODAFONE_DW");
	    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
	    requestData.add(new BasicNameValuePair("receiverNumber", receiver));
	    requestData.add(new BasicNameValuePair("message", message));
	    request.setEntity(new UrlEncodedFormEntity(requestData,
		    HTTP.ISO_8859_1));
	    HttpResponse response = httpClient.execute(request, httpContext);
	    document = new SAXBuilder()
		    .build(response.getEntity().getContent());
	    response.getEntity().consumeContent();
	} catch (Exception e) {
	    throw new Exception(
		    context.getString(R.string.WebSenderNetworkError));
	}

	Element root = document.getRootElement();
	@SuppressWarnings("unchecked")
	List<Element> children = root.getChildren("e");
	Log.d("VodafoneIT", "doPrepare()");
	int status = 0, errorcode = 0;
	for (Element child : children) {
	    Log.d("VodafoneIT", child.getAttributeValue("n"));
	    if (child.getAttributeValue("v") != null)
		Log.d("VodafoneIT", child.getAttributeValue("v"));
	    if (child.getValue() != null)
		Log.d("VodafoneIT", child.getValue());
	    if (child.getAttributeValue("n").equals("STATUS"))
		status = Integer.parseInt(child.getAttributeValue("v"));
	    if (child.getAttributeValue("n").equals("ERRORCODE"))
		errorcode = Integer.parseInt(child.getAttributeValue("v"));
	    if (child.getAttributeValue("n").equals("CODEIMG")) {
		captchaArray = Base64.decode(child.getValue());
		return false;
	    }
	}

	Log.d("VodafoneIT", "status code: " + status);
	Log.d("VodafoneIT", "error code: " + errorcode);
	if (status != 1)
	    parseError(errorcode);
	
	return true;
    }

    /**
     * Send the message (after decoding the CAPTCHA)
     * 
     * @param receiver
     * @param message
     * @param captcha
     * @throws Exception
     * @returns false if CAPTCHA still present
     */
    private boolean doSend(String receiver, String message, String captcha)
	    throws Exception {
	Document document;

	try {
	    HttpPost request = new HttpPost(
		    "https://widget.vodafone.it/190/fsms/send.do?channel=VODAFONE_DW");
	    List<NameValuePair> requestData = new ArrayList<NameValuePair>();
	    requestData.add(new BasicNameValuePair("verifyCode", captcha));
	    requestData.add(new BasicNameValuePair("receiverNumber", receiver));
	    requestData.add(new BasicNameValuePair("message", message));
	    request.setEntity(new UrlEncodedFormEntity(requestData,
		    HTTP.ISO_8859_1));
	    HttpResponse response = httpClient.execute(request, httpContext);
	    document = new SAXBuilder()
		    .build(response.getEntity().getContent());
	    response.getEntity().consumeContent();
	} catch (Exception e) {
	    throw new Exception(
		    context.getString(R.string.WebSenderNetworkError));
	}

	Element root = document.getRootElement();
	@SuppressWarnings("unchecked")
	List<Element> children = root.getChildren("e");
	Log.d("VodafoneIT", "doSend()");
	int status = 0, errorcode = 0;
	String returnmsg = null;
	for (Element child : children) {
	    Log.d("VodafoneIT", child.getAttributeValue("n"));
	    if (child.getAttributeValue("v") != null)
		Log.d("VodafoneIT", child.getAttributeValue("v"));
	    if (child.getValue() != null)
		Log.d("VodafoneIT", child.getValue());
	    if (child.getAttributeValue("n").equals("STATUS"))
		status = Integer.parseInt(child.getAttributeValue("v"));
	    if (child.getAttributeValue("n").equals("ERRORCODE"))
		errorcode = Integer.parseInt(child.getAttributeValue("v"));
	    if (child.getAttributeValue("n").equals("RETURNMSG"))
		returnmsg = child.getValue();
	    if (child.getAttributeValue("n").equals("CODEIMG")) {
		captchaArray = Base64.decode(child.getValue());
		return false;
	    }
	}

	Log.d("VodafoneIT", "status code: " + status);
	Log.d("VodafoneIT", "error code: " + errorcode);
	Log.d("VodafoneIT", "return message: " + returnmsg);
	if (status != 1)
	    parseError(errorcode);
	
	return true;
    }

}
