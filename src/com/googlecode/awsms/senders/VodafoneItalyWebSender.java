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

/**
 * <code>WebSender</code> implementation for www.vodafone.it web site
 * 
 * @author Andrea De Pasquale
 */
public class VodafoneItalyWebSender extends WebSender {
	
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
	
//	XMLs containing information about the widget itself
//	http://demos.vodafone.it/dw/getDWConfiguration.xml
//	http://demos.vodafone.it/dw/updates.xml
		
	public VodafoneItalyWebSender() {
		super();
		name = "Vodafone IT";
		dailyLimit = 10;
		httpClient.getParams().setParameter("http.protocol.allow-circular-redirects", true);
		httpClient.getParams().setParameter("http.useragent", "Vodafone_DW");
		
		try {
			SSLSocketFactory socketFactory = new SSLSocketFactory(null);
			Scheme sch = new Scheme("https", socketFactory, 443);
			httpClient.getConnectionManager().getSchemeRegistry().register(sch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void send(
			String username, String password, 
			String receiver, String message, String captcha) throws Exception {
		
		// TODO pop-up message for username or password not set
		if (username.equals("") || password.equals("")) {
			throw new Exception("invalid settings");
		}
		
		// TODO disable button when receiver does not satisfy this
		receiver = stripPrefix("+39", receiver);
		if (receiver.length() < 9 || receiver.length() > 10) {
			throw new Exception("invalid receiver");
		}
		
		if (captcha.equals("")) {
			if (!isLoggedIn()) doLogin(username, password);
			doPrecheck();
			doPrepare(receiver, message);
		}
		
		doSend(receiver, message, captcha);
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
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	private boolean isLoggedIn() throws Exception {
		try {
			HttpGet request = new HttpGet("https://widget.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp");
			HttpResponse response = httpClient.execute(request, httpContext);
			Document document = new SAXBuilder().build(response.getEntity().getContent());
			response.getEntity().consumeContent();
			Element root = document.getRootElement();
			Element child = root.getChild("logged-in");
			return child.getValue().equals("true");
		} catch (Exception e) {
			throw new Exception("network error");
		}
	}
	
	/**
	 * 
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	private void doLogin(String username, String password) throws Exception {
		try {
			HttpPost request = new HttpPost("https://widget.vodafone.it/190/trilogy/jsp/login.do");
			List<NameValuePair> requestData = new ArrayList<NameValuePair>();
			requestData.add(new BasicNameValuePair("username", username));
			requestData.add(new BasicNameValuePair("password", password));
			request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.ISO_8859_1));
			HttpResponse response = httpClient.execute(request, httpContext);
			response.getEntity().consumeContent();
		} catch (Exception e) {
			throw new Exception("network error");
		}
		
		if (!isLoggedIn()) {
			throw new Exception("invalid settings");
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private void doPrecheck() throws Exception {
		Document document;
		
		try {
			HttpGet request = new HttpGet("https://widget.vodafone.it/190/fsms/precheck.do?channel=VODAFONE_DW");
			HttpResponse response = httpClient.execute(request, httpContext);
			
			Reader reader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			reader.skip(13); // this trick solves XML header problem
			
			document = new SAXBuilder().build(reader);		
			response.getEntity().consumeContent();
			
		} catch (Exception e) {
			throw new Exception("network error");
		}
		
		Element root = document.getRootElement();
		List<Element> children = root.getChildren("e");
		for (Element child : children) {
			if (child.getAttributeValue("n").equals("STATUS")) {
				if (!child.getAttributeValue("v").equals("1")) {
					throw new Exception("unknown error");
				}
			}
			if (child.getAttributeValue("n").equals("ERRORCODE")) {
				// TODO parse error codes (must first discover them)
				if (!child.getAttributeValue("v").equals("0")) {
					throw new Exception("unknown error");
				}
			}
		}
	}
	
	/**
	 * 
	 * @param receiver
	 * @param message
	 * @throws Exception
	 */
	private void doPrepare(String receiver, String message) throws Exception {
		Document document;
		
		try {
			HttpPost request = new HttpPost("https://widget.vodafone.it/190/fsms/prepare.do?channel=VODAFONE_DW");
			List<NameValuePair> requestData = new ArrayList<NameValuePair>();
			requestData.add(new BasicNameValuePair("receiverNumber", receiver));
			requestData.add(new BasicNameValuePair("message", message));
			request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.ISO_8859_1));
			HttpResponse response = httpClient.execute(request, httpContext);
			document = new SAXBuilder().build(response.getEntity().getContent());
			response.getEntity().consumeContent();
		} catch (Exception e) {
			throw new Exception("network error");
		}
		
		Element root = document.getRootElement();
		List<Element> children = root.getChildren("e");
		for (Element child : children) {
			if (child.getAttributeValue("n").equals("STATUS")) {
				if (!child.getAttributeValue("v").equals("1")) {
					throw new Exception("unknown error");
				}
			}
			if (child.getAttributeValue("n").equals("ERRORCODE")) {
				// TODO parse error codes (must first discover them)
				if (!child.getAttributeValue("v").equals("0")) {
					throw new Exception("unknown error");
				}
			}
			if (child.getAttributeValue("n").equals("CODEIMG")) {
				// TODO decode the captcha using JavaOCR
				captchaArray = Base64.decode(child.getValue());
				throw new Exception("need captcha");
			}
		}
	}
	
	/**
	 * 
	 * @param receiver
	 * @param message
	 * @param captcha
	 * @throws Exception
	 */
	private void doSend(String receiver, String message, String captcha) throws Exception {
		Document document;
		
		try {
			HttpPost request = new HttpPost("https://widget.vodafone.it/190/fsms/send.do?channel=VODAFONE_DW");
			List<NameValuePair> requestData = new ArrayList<NameValuePair>();
			requestData.add(new BasicNameValuePair("verifyCode", captcha));
			requestData.add(new BasicNameValuePair("receiverNumber", receiver));
			requestData.add(new BasicNameValuePair("message", message));
			request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.ISO_8859_1));
			HttpResponse response = httpClient.execute(request, httpContext);
			document = new SAXBuilder().build(response.getEntity().getContent());
			response.getEntity().consumeContent();
		} catch (Exception e) {
			throw new Exception("network error");
		}
		
		Element root = document.getRootElement();
		List<Element> children = root.getChildren("e");
		boolean sent = false; 
		for (Element child : children) {
			if (child.getAttributeValue("n").equals("STATUS")) {
				if (!child.getAttributeValue("v").equals("1")) {
					throw new Exception("unknown error");
				}
			}
			if (child.getAttributeValue("n").equals("ERRORCODE")) {
				// TODO parse error codes (must first discover them)
				if (!child.getAttributeValue("v").equals("0")) {
					throw new Exception("unknown error");
				}
			}
			if (child.getAttributeValue("n").equals("RETURNMSG")) {
				sent = true; // TODO untested
			}
		}
		
		if (!sent) {
			throw new Exception("unknown error");
		}
	}

}
	
