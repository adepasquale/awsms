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
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

/**
 * <code>WebSender</code> implementation for www.vodafone.it web site
 * 
 * @author Andrea De Pasquale
 */
public class VodafoneItalyWebSender extends WebSender {

	// TODO try doing the same as Vodafone widget

	public Result send(
			String username, String password, 
			String receiver, String message, String captcha) {
		
		if (username.equals("") || password.equals("")) {
			return Result.SETTINGS_INVALID;
		}
		
		receiver = stripPrefix("+39", receiver);
		if (receiver.length() < 9 || receiver.length() > 10) {
			return Result.RECEIVER_INVALID;
		}
		
		if (getInformation(message.length())[0] == 0) {
			return Result.MESSAGE_INVALID;
		}
		
		try {
		
			if (captcha.equals("")) {
				
				HttpGet reqHome = new HttpGet("http://www.vodafone.it/190/trilogy/jsp/home.do");
				HttpResponse resHome = httpClient.execute(reqHome, httpContext);
				String sHome = streamToString(resHome.getEntity().getContent());
				resHome.getEntity().consumeContent();
				if (isWebsiteUnavailable(sHome)) return Result.WEBSITE_UNAVAILABLE;
			
				if (!isUserLoggedIn(sHome)) {
					HttpPost reqLogin = new HttpPost("https://www.vodafone.it/190/trilogy/jsp/login.do");
					List<NameValuePair> reqLoginData = new ArrayList<NameValuePair>();
					reqLoginData.add(new BasicNameValuePair("username", username));
					reqLoginData.add(new BasicNameValuePair("password", password));
					reqLogin.setEntity(new UrlEncodedFormEntity(reqLoginData, HTTP.ISO_8859_1));
					HttpResponse resLogin = httpClient.execute(reqLogin, httpContext);
					String sLogin = streamToString(resLogin.getEntity().getContent());
					resLogin.getEntity().consumeContent();
					if (isWebsiteUnavailable(sLogin)) return Result.WEBSITE_UNAVAILABLE;
					if (!isUserLoggedIn(sLogin)) return Result.SETTINGS_INVALID;
				}
			
				HttpGet reqAdv = new HttpGet("http://www.areaprivati.vodafone.it/190/trilogy/jsp/dispatcher.do?ty_key=fdt_pri_sms1&tk=9609,c");
				HttpResponse resAdv = httpClient.execute(reqAdv, httpContext);
				String sAdv = streamToString(resAdv.getEntity().getContent());
				resAdv.getEntity().consumeContent();
				if (isWebsiteUnavailable(sAdv)) return Result.WEBSITE_UNAVAILABLE;
				
				HttpGet reqSMS = new HttpGet("http://www.areaprivati.vodafone.it/190/trilogy/jsp/dispatcher.do?ty_key=fsms_hp&amp;ipage=next");
				HttpResponse resSMS = httpClient.execute(reqSMS, httpContext);
				String sSMS = streamToString(resSMS.getEntity().getContent());
				resSMS.getEntity().consumeContent();
				if (isWebsiteUnavailable(sSMS)) return Result.WEBSITE_UNAVAILABLE;
			
				HttpPost reqPrep = new HttpPost("http://www.areaprivati.vodafone.it/190/fsms/prepare.do");
				List<NameValuePair> reqPrepData = new ArrayList<NameValuePair>();
				reqPrepData.add(new BasicNameValuePair("pageTypeId", "9604"));
				reqPrepData.add(new BasicNameValuePair("programId", "10384"));
				reqPrepData.add(new BasicNameValuePair("chanelId", "-18126"));
				reqPrepData.add(new BasicNameValuePair("receiverNumber", receiver));
				reqPrepData.add(new BasicNameValuePair("message", message));
				reqPrep.setEntity(new UrlEncodedFormEntity(reqPrepData, HTTP.ISO_8859_1));
				HttpResponse resPrep = httpClient.execute(reqPrep, httpContext);
				String sPrep = streamToString(resPrep.getEntity().getContent());
				resPrep.getEntity().consumeContent();
				if (isWebsiteUnavailable(sPrep)) return Result.WEBSITE_UNAVAILABLE;
				if (isInvalidReceiver(sPrep)) return Result.RECEIVER_INVALID;
				if (isOutOfMessages(sPrep)) return Result.OUT_OF_MESSAGES;
				
				if (isCaptchaPresent(sPrep)) {
					HttpGet reqCaptcha = new HttpGet("http://www.areaprivati.vodafone.it/190/fsms/generateimg.do");
					HttpResponse resCaptcha = httpClient.execute(reqCaptcha, httpContext);
					captchaArray = streamToByteArray(resCaptcha.getEntity().getContent());
					return Result.NEED_CAPTCHA; // TODO decode the captcha using JavaOCR
				}
				
			}
			
			HttpPost reqSend = new HttpPost("http://www.areaprivati.vodafone.it/190/fsms/send.do");
			List<NameValuePair> reqSendData = new ArrayList<NameValuePair>();
			reqSendData.add(new BasicNameValuePair("verifyCode", captcha));
			reqSendData.add(new BasicNameValuePair("pageTypeId", "9604"));
			reqSendData.add(new BasicNameValuePair("programId", "10384"));
			reqSendData.add(new BasicNameValuePair("chanelId", "-18126"));
			reqSendData.add(new BasicNameValuePair("receiverNumber", receiver));
			reqSendData.add(new BasicNameValuePair("message", message));
			reqSend.setEntity(new UrlEncodedFormEntity(reqSendData, HTTP.ISO_8859_1));
			HttpResponse resSend = httpClient.execute(reqSend, httpContext);
			String sSend = streamToString(resSend.getEntity().getContent());
			resSend.getEntity().consumeContent();
			if (isOutOfMessages(sSend)) return Result.OUT_OF_MESSAGES;
			if (!isMessageSent(sSend)) return Result.UNKNOWN_ERROR;
			
			return Result.MESSAGE_SENT;
		
		} catch (Exception e) {
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
	
	// TODO find an efficient method to search for a string in a stream 
	private String streamToString(InputStream is) {
		try {
			
			final int SIZE = 64*1024;
			
			Writer output = new StringWriter(SIZE);
			char[] buffer = new char[1024];
			Reader input = new BufferedReader(
					new InputStreamReader(is, HTTP.ISO_8859_1), SIZE);
			
			int bytes;
			while ((bytes = input.read(buffer)) != -1) {
				output.write(buffer, 0, bytes);
			}
	
			is.close();
			output.flush();
			return output.toString();
			
		} catch (Exception e) {
			return null;
		}
	}
	
	private boolean findString(String what, String into) {
		if (into.indexOf(what) != -1) return true;
		else return false;
	}
	
	private boolean isWebsiteUnavailable(String response) {
		return findString("intervento di manutenzione", response) || 
				findString("numero massimo di accessi", response);
	}
	
	private boolean isUserLoggedIn(String response) {
		return findString("logout.do", response);
	}
	
	private boolean isInvalidReceiver(String response) {
		return findString("solo a numeri di cellulare Vodafone", response) ||
				findString("destinatario del messaggio non e' valido", response);
	}
	
	private boolean isOutOfMessages(String response) {
		return findString("box_sup_limitesms.gif", response) || 
				findString("superato il limite giornaliero", response);
	}
	
	private boolean isCaptchaPresent(String response) {
		return findString("generateimg.do", response);
	}
	
	private boolean isMessageSent(String response) {
		return findString("elaborata correttamente", response);
	}
}
	
