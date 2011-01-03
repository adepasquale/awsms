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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Abstract class to help WebSender in common tasks  
 * 
 * @author Andrea De Pasquale
 */
public abstract class WebSenderHelper {
    
    protected Context context;
    protected WebSenderDatabase database;
    protected SharedPreferences preferences;
    
    public WebSenderHelper(Context context) {
	this.context = context;
	database = new WebSenderDatabase(context);
	preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public abstract String getUsername();
    public abstract String getPassword();

    /**
     * Retrieve remaining characters.
     * 
     * @param length Length of the text message.
     * @return How many characters are remaining.
     */
    public abstract int calcRemaining(int length);
    
    /**
     * Retrieve number of fragments.
     * 
     * @param length Length of the text message.
     * @return How many messages will be sent.
     */
    public abstract int calcFragments(int length);

    /**
     * Increment number of messages sent today.
     * 
     * @param length Length of the text message.
     */
    public abstract void addCount(int length);
    
    /**
     * Retrieve number of messages sent today.
     * 
     * @return How many messages have been sent today.
     */
    public abstract int getCount();
    
    /**
     * Retrieve daily sending limit.
     * 
     * @return how many messages can be sent in a single day.
     */    
    public abstract int getLimit();
}
