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

    String[] receivers;
    String message;
    
    /**
     * Create a text message with a single receiver
     * 
     * @param receiver Phone number of the message addressee.
     * @param message Text message to be sent to the receiver.
     */
    public WebSMS(String receiver, String message) {
	this.receivers = new String[1];
	this.receivers[0] = receiver;
	this.message = message;
    }

    /**
     * Create a text message with multiple receivers
     * 
     * @param receivers Phone numbers of the many message addressees.
     * @param message Text message to be sent to every receiver.
     */
    public WebSMS(String[] receivers, String message) {
	this.receivers = receivers;
	this.message = message;
    }

    public String[] getReceivers() {
        return receivers;
    }

    public String getMessage() {
        return message;
    }
    
}
