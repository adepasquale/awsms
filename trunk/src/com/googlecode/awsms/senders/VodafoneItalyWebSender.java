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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import android.util.Log;

/**
 * <code>WebSender</code> implementation for www.vodafone.it web site
 * 
 * @author Andrea De Pasquale
 */
// TODO use the widget method to send, it's much faster
public class VodafoneItalyWebSender extends WebSender {
	
//	XMLs containing information about the widget itself
//	http://demos.vodafone.it/dw/getDWConfiguration.xml
//	http://demos.vodafone.it/dw/updates.xml
	
//	URLs used by the widget to login/logout, send SMS, etc.
//	https://widget.vodafone.it/190/trilogy/jsp/login.do
//	https://widget.vodafone.it/190/trilogy/jsp/logout.do
//	https://widget.vodafone.it/190/trilogy/jsp/swapSim.do
//	https://widget.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp
//	https://widget.vodafone.it/190/fsms/precheck.do?channel=VODAFONE_DW
//	https://widget.vodafone.it/190/fsms/prepare.do?channel=VODAFONE_DW
//	https://widget.vodafone.it/190/fsms/send.do?channel=VODAFONE_DW
//	https://widget.vodafone.it/190/fast/mx/CreditoResiduoPush.do?hpfdtpri=y
//	https://widget.vodafone.it/190/jone/mx/SaldoPuntiPush.do?hpfdtpri=y
//	https://widget.vodafone.it/190/ebwe/mx/PushInfoconto.do?hpfdtpri=y
		
	public VodafoneItalyWebSender() {
		super();
		name = "Vodafone IT";
		dailyLimit = 10;
		httpClient.getParams().setParameter("http.useragent", "Vodafone_DW");
		try {
			SSLSocketFactory socketFactory = new SSLSocketFactory(null);
	        Scheme sch = new Scheme("https", socketFactory, 443);
	        httpClient.getConnectionManager().getSchemeRegistry().register(sch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Result send(
			String username, String password, 
			String receiver, String message, String captcha) {
		
		if (username.equals("") || password.equals("")) {
			return Result.INVALID_SETTINGS;
		}
		
		receiver = stripPrefix("+39", receiver);
		if (receiver.length() < 9 || receiver.length() > 10) {
			return Result.INVALID_RECEIVER;
		}
		
		if (getInformation(message.length())[0] == 0) {
			return Result.INVALID_MESSAGE;
		}
		
		try {
		
			if (captcha.equals("")) {
							
				if (!isLoggedIn()) {
					doLogin(username, password);
					if (!isLoggedIn()) { 
						return Result.INVALID_SETTINGS;
					}
				}
				
				if (!doPrecheck()) {
					return Result.WEBSITE_UNAVAILABLE;
				}
			
//				HttpPost reqPrep = new HttpPost("http://www.areaprivati.vodafone.it/190/fsms/prepare.do");
//				List<NameValuePair> reqPrepData = new ArrayList<NameValuePair>();
//				reqPrepData.add(new BasicNameValuePair("pageTypeId", "9604"));
//				reqPrepData.add(new BasicNameValuePair("programId", "10384"));
//				reqPrepData.add(new BasicNameValuePair("chanelId", "-18126"));
//				reqPrepData.add(new BasicNameValuePair("receiverNumber", receiver));
//				reqPrepData.add(new BasicNameValuePair("message", message));
//				reqPrep.setEntity(new UrlEncodedFormEntity(reqPrepData, HTTP.ISO_8859_1));
//				HttpResponse resPrep = httpClient.execute(reqPrep, httpContext);
//				String sPrep = streamToString(resPrep.getEntity().getContent());
//				resPrep.getEntity().consumeContent();
//				if (isWebsiteUnavailable(sPrep)) return Result.WEBSITE_UNAVAILABLE;
//				if (isInvalidReceiver(sPrep)) return Result.INVALID_RECEIVER;
//				if (isOutOfMessages(sPrep)) return Result.OUT_OF_MESSAGES;
//				
//				if (isCaptchaPresent(sPrep)) {
//					HttpGet reqCaptcha = new HttpGet("http://www.areaprivati.vodafone.it/190/fsms/generateimg.do");
//					HttpResponse resCaptcha = httpClient.execute(reqCaptcha, httpContext);
//					captchaArray = streamToByteArray(resCaptcha.getEntity().getContent());
//					return Result.NEED_CAPTCHA; // TODO decode the captcha using JavaOCR
//				}
				
			}
			
//			HttpPost reqSend = new HttpPost("http://www.areaprivati.vodafone.it/190/fsms/send.do");
//			List<NameValuePair> reqSendData = new ArrayList<NameValuePair>();
//			reqSendData.add(new BasicNameValuePair("verifyCode", captcha));
//			reqSendData.add(new BasicNameValuePair("pageTypeId", "9604"));
//			reqSendData.add(new BasicNameValuePair("programId", "10384"));
//			reqSendData.add(new BasicNameValuePair("chanelId", "-18126"));
//			reqSendData.add(new BasicNameValuePair("receiverNumber", receiver));
//			reqSendData.add(new BasicNameValuePair("message", message));
//			reqSend.setEntity(new UrlEncodedFormEntity(reqSendData, HTTP.ISO_8859_1));
//			HttpResponse resSend = httpClient.execute(reqSend, httpContext);
//			String sSend = streamToString(resSend.getEntity().getContent());
//			resSend.getEntity().consumeContent();
//			if (isOutOfMessages(sSend)) return Result.OUT_OF_MESSAGES;
//			if (!isMessageSent(sSend)) return Result.UNKNOWN_ERROR;
			
			return Result.MESSAGE_SENT;
		
		} catch (Exception e) {
			e.printStackTrace();
			return Result.UNKNOWN_ERROR;
		}
	}
	
	public int[] getInformation(int messageLength) {
		int[] info = new int[2]; 
		
		if (messageLength == 0) {
			info[0] = 0;
			info[1] = 0;
		} else if (messageLength <= 160) {
			info[0] = 1;
			info[1] = 160-messageLength;
		} else if (messageLength <= 307) {
			info[0] = 2;
			info[1] = 307-messageLength;
		} else if (messageLength <= 360) {
			info[0] = 3;
			info[1] = 360-messageLength;
		} else {
			info[0] = 0;
			info[1] = 360-messageLength;
		}
		
		return info;
	}
	
	private boolean isLoggedIn() throws Exception {
		HttpGet request = new HttpGet("https://widget.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp");
		HttpResponse response = httpClient.execute(request, httpContext);
		Document document = new SAXBuilder().build(response.getEntity().getContent());
		response.getEntity().consumeContent();
		
		Element root = document.getRootElement();
		Element child = root.getChild("logged-in");
		Log.d("VodafoneIT", "logged-in: " + child.getValue());
		return child.getValue().equals("true");
	}
	
	private void doLogin(String username, String password) throws Exception {
		HttpPost request = new HttpPost("https://widget.vodafone.it/190/trilogy/jsp/login.do");
		List<NameValuePair> requestData = new ArrayList<NameValuePair>();
		requestData.add(new BasicNameValuePair("username", username));
		requestData.add(new BasicNameValuePair("password", password));
		request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.ISO_8859_1));
		HttpResponse response = httpClient.execute(request, httpContext);
		response.getEntity().consumeContent();
		Log.d("VodafoneIT", "logging in with " + username + " and " + password);
	}
	
	private boolean doPrecheck() throws Exception {
		HttpGet request = new HttpGet("https://widget.vodafone.it/190/fsms/precheck.do?channel=VODAFONE_DW");
		HttpResponse response = httpClient.execute(request, httpContext);
		
		// FIXME this trick should solve XML header problem, but it doesn't
		Reader reader = new BufferedReader(
				new InputStreamReader(response.getEntity().getContent()));
		reader.skip(13);
		
		Document document = new SAXBuilder().build(reader);
		response.getEntity().consumeContent();
		
		Element root = document.getRootElement();
		List<Element> children = root.getChildren("e");
		for (Element child : children) {
			Log.d("VodafoneIT", "" + child.getAttributeValue("n"));
			Log.d("VodafoneIT", "" + child.getAttributeValue("v"));
		}
		return false;
	}
	
	private byte[] streamToByteArray(InputStream is) {
		try {
			
			final int SIZE = 4*1024;
			
			ByteArrayOutputStream output = new ByteArrayOutputStream(SIZE);
			byte[] buffer = new byte[1024];
			
			int bytes;
			while ((bytes = is.read(buffer, 0, buffer.length)) != -1) {
				output.write(buffer, 0, bytes);
			}
	
			is.close();
			output.flush();
			return output.toByteArray();
			
		} catch (Exception e) {
			return null;
		}
	}

}
	
