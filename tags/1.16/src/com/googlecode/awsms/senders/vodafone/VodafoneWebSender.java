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

import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
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

    helper = new VodafoneWebSenderHelper(context);
  }

  public void preSend() throws Exception {
    Log.d(TAG, "this.preSend()");
    if (!isLoggedIn())
      doLogin();
  }

  public boolean send(WebSMS sms) throws Exception {
    Log.d(TAG, "this.send()");

    if (sms.getCaptchaArray() == null) {
      Log.d(TAG, "sms.getCaptchaArray() == null");
      preSend();
      doPrecheck();
      if (!doPrepare(sms))
        return false; // need CAPTCHA
    }

    if (!doSend(sms))
      return false; // still need CAPTCHA
    helper.addCount(sms.getMessage().length());
    return true;
  }

  /**
   * Check if the user is logged in to www.vodafone.it
   * 
   * @return true if the user is logged in, false otherwise
   * @throws Exception
   */
  private boolean isLoggedIn() throws Exception {
    Log.d(TAG, "this.isLoggedIn()");
    Document document;

    try {
      HttpGet request = new HttpGet(
          "https://www.vodafone.it/190/trilogy/jsp/utility/checkUser.jsp");
      HttpResponse response = httpClient.execute(request, httpContext);
      
      PushbackReader reader = new PushbackReader(new InputStreamReader(
          response.getEntity().getContent()));
      
      // fix wrong XML header 
      int first = reader.read();
      while (first != 60)
        first = reader.read();
      reader.unread(first);

      document = new SAXBuilder().build(reader);
      response.getEntity().consumeContent();

    } catch (JDOMException jdom) {
      throw new Exception(context.getString(R.string.WebSenderProtocolError));
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception(context.getString(R.string.WebSenderNetworkError));
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
    Log.d(TAG, "this.doLogin()");

    try {
      HttpPost request = new HttpPost(
          "https://www.vodafone.it/190/trilogy/jsp/login.do");
      List<NameValuePair> requestData = new ArrayList<NameValuePair>();
      requestData.add(new BasicNameValuePair("username", helper.getUsername()));
      requestData.add(new BasicNameValuePair("password", helper.getPassword()));
      request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(request, httpContext);
      response.getEntity().consumeContent();
    } catch (Exception e) {
      throw new Exception(context.getString(R.string.WebSenderNetworkError));
    }

    if (!isLoggedIn()) {
      throw new Exception(context.getString(R.string.WebSenderSettingsInvalid));
    }

    saveCookies(); // to cookie file
  }

  private void parseError(int error) throws Exception {
    switch (error) {
    case 107:
      throw new Exception(context.getString(R.string.WebSenderLimitReached));

    case 113:
      throw new Exception(
          context.getString(R.string.WebSenderReceiverNotAllowed));

    case 104: // service unavailable
    case 109: // empty message
    default:
      throw new Exception(context.getString(R.string.WebSenderUnknownError));
    }
  }

  /**
   * Page to be visited before sending a message
   * 
   * @throws Exception
   */
  private void doPrecheck() throws Exception {
    Log.d(TAG, "this.doPrecheck()");
    Document document = null;

    try {
      HttpGet request = new HttpGet(
          "https://www.vodafone.it/190/fsms/precheck.do?channel=VODAFONE_DW");
      HttpResponse response = httpClient.execute(request, httpContext);

      PushbackReader reader = new PushbackReader(new InputStreamReader(
          response.getEntity().getContent()));
      
      // fix wrong XML header 
      int first = reader.read();
      while (first != 60)
        first = reader.read();
      reader.unread(first);

      document = new SAXBuilder().build(reader);
      response.getEntity().consumeContent();

    } catch (JDOMException jdom) {
      throw new Exception(context.getString(R.string.WebSenderProtocolError));
    } catch (Exception e) {
      throw new Exception(context.getString(R.string.WebSenderNetworkError));
    }

    Element root = document.getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> children = root.getChildren("e");
    int status = 0, errorcode = 0;
    for (Element child : children) {
//      Log.d(TAG, child.getAttributeValue("n"));
//      if (child.getAttributeValue("v") != null)
//        Log.d(TAG, child.getAttributeValue("v"));
//      if (child.getValue() != null)
//        Log.d(TAG, child.getValue());
      if (child.getAttributeValue("n").equals("STATUS"))
        status = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("ERRORCODE"))
        errorcode = Integer.parseInt(child.getAttributeValue("v"));
    }

    Log.d(TAG, "status code: " + status);
    Log.d(TAG, "error code: " + errorcode);
    if (status != 1)
      parseError(errorcode);
  }

  /**
   * Prepare the message to be sent.
   * 
   * @param sms
   * @throws Exception
   * @returns false if CAPTCHA present
   */
  private boolean doPrepare(WebSMS sms) throws Exception {
    Log.d(TAG, "this.doPrepare()");
    Document document;

    try {
      HttpPost request = new HttpPost(
          "https://www.vodafone.it/190/fsms/prepare.do?channel=VODAFONE_DW");
      List<NameValuePair> requestData = new ArrayList<NameValuePair>();
      requestData.add(new BasicNameValuePair("receiverNumber", sms
          .getReceiverNumber()));
      requestData.add(new BasicNameValuePair("message", sms.getMessage()));
      request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(request, httpContext);
      
      PushbackReader reader = new PushbackReader(new InputStreamReader(
          response.getEntity().getContent()));
      
      // fix wrong XML header 
      int first = reader.read();
      while (first != 60)
        first = reader.read();
      reader.unread(first);

      document = new SAXBuilder().build(reader);
      response.getEntity().consumeContent();

    } catch (JDOMException jdom) {
      throw new Exception(context.getString(R.string.WebSenderProtocolError));
    } catch (Exception e) {
      throw new Exception(context.getString(R.string.WebSenderNetworkError));
    }

    Element root = document.getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> children = root.getChildren("e");
    int status = 0, errorcode = 0;
    for (Element child : children) {
//      Log.d(TAG, child.getAttributeValue("n"));
//      if (child.getAttributeValue("v") != null)
//        Log.d(TAG, child.getAttributeValue("v"));
//      if (child.getValue() != null)
//        Log.d(TAG, child.getValue());
      if (child.getAttributeValue("n").equals("STATUS"))
        status = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("ERRORCODE"))
        errorcode = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("CODEIMG")) {
        sms.setCaptchaArray(Base64.decode(child.getValue()));
        return false;
      }
    }

    Log.d(TAG, "status code: " + status);
    Log.d(TAG, "error code: " + errorcode);
    if (status != 1)
      parseError(errorcode);

    return true;
  }

  /**
   * Send the message (after decoding the CAPTCHA)
   * 
   * @param sms
   * @throws Exception
   * @returns false if CAPTCHA still present
   */
  private boolean doSend(WebSMS sms) throws Exception {
    Log.d(TAG, "this.doSend()");
    Document document;

    try {
      HttpPost request = new HttpPost(
          "https://www.vodafone.it/190/fsms/send.do?channel=VODAFONE_DW");
      List<NameValuePair> requestData = new ArrayList<NameValuePair>();
      requestData.add(new BasicNameValuePair("verifyCode", sms.getCaptcha()));
      requestData.add(new BasicNameValuePair("receiverNumber", sms
          .getReceiverNumber()));
      requestData.add(new BasicNameValuePair("message", sms.getMessage()));
      request.setEntity(new UrlEncodedFormEntity(requestData, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(request, httpContext);
      
      PushbackReader reader = new PushbackReader(new InputStreamReader(
          response.getEntity().getContent()));
      
      // fix wrong XML header 
      int first = reader.read();
      while (first != 60)
        first = reader.read();
      reader.unread(first);

      document = new SAXBuilder().build(reader);
      response.getEntity().consumeContent();

    } catch (JDOMException jdom) {
      throw new Exception(context.getString(R.string.WebSenderProtocolError));
    } catch (Exception e) {
      throw new Exception(context.getString(R.string.WebSenderNetworkError));
    }

    Element root = document.getRootElement();
    @SuppressWarnings("unchecked")
    List<Element> children = root.getChildren("e");
    int status = 0, errorcode = 0;
    String returnmsg = null;
    for (Element child : children) {
//      Log.d(TAG, child.getAttributeValue("n"));
//      if (child.getAttributeValue("v") != null)
//        Log.d(TAG, child.getAttributeValue("v"));
//      if (child.getValue() != null)
//        Log.d(TAG, child.getValue());
      if (child.getAttributeValue("n").equals("STATUS"))
        status = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("ERRORCODE"))
        errorcode = Integer.parseInt(child.getAttributeValue("v"));
      if (child.getAttributeValue("n").equals("RETURNMSG"))
        returnmsg = child.getValue();
      if (child.getAttributeValue("n").equals("CODEIMG")) {
        sms.setCaptchaArray(Base64.decode(child.getValue()));
        return false;
      }
    }

    Log.d(TAG, "status code: " + status);
    Log.d(TAG, "error code: " + errorcode);
    Log.d(TAG, "return message: " + returnmsg);
    if (status != 1)
      parseError(errorcode);

    return true;
  }

}
