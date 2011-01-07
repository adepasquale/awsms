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

/**
 * Simple class representing a Web SMS
 * 
 * @author Andrea De Pasquale
 */
public class WebSMS {
    // TODO support multiple receivers
    // TODO encapsulate status and error/return codes 

    String sender;
    String senderName;
    String receiver;
    String receiverName;
    String message;

    // XXX remove
    byte[] captchaArray;
    String captcha;

    public String getSender() {
	return sender;
    }

    public void setSender(String sender) {
	this.sender = sender;
    }

    public String getSenderName() {
	return senderName;
    }

    public void setSenderName(String senderName) {
	this.senderName = senderName;
    }

    public String getReceiver() {
	return receiver;
    }

    public void setReceiver(String receiver) {
	this.receiver = receiver;
    }

    public String getReceiverName() {
	return receiverName;
    }

    public void setReceiverName(String receiverName) {
	this.receiverName = receiverName;
    }

    public String getMessage() {
	return message;
    }

    public void setMessage(String message) {
	this.message = message;
    }

    public byte[] getCaptchaArray() {
	return captchaArray;
    }

    public void setCaptchaArray(byte[] captchaArray) {
	this.captchaArray = captchaArray;
    }

    public String getCaptcha() {
	return captcha;
    }

    public void setCaptcha(String captcha) {
	this.captcha = captcha;
    }

}
