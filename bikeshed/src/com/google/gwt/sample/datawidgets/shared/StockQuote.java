/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.sample.datawidgets.shared;

import java.io.Serializable;

/**
 * A single stock quote.
 */
public class StockQuote implements Serializable {

  private transient boolean favorite;
  private String name;
  private transient String notes;
  private int price;
  private String ticker;

  public StockQuote(StockQuote toCopy) {
    ticker = toCopy.ticker;
    name = toCopy.name;
    price = toCopy.price;
    setNotes(toCopy.getNotes());
  }

  /**
   * Construct a new {@link StockQuote}.
   * 
   * @param ticker the ticket symbol
   * @param name the company name
   * @param price the price in pennies
   */
  public StockQuote(String ticker, String name, int price) {
    this.ticker = ticker;
    this.name = name;
    this.price = price;
  }
  
  /**
   * Used for RPC.
   */
  StockQuote() {
  }

  public String getDisplayPrice() {
    int dollars = getPrice() / 100;
    int cents = getPrice() % 100;
    
    StringBuilder sb = new StringBuilder();
    sb.append("$ ");
    sb.append(dollars);
    sb.append('.');
    if (cents < 10) {
      sb.append('0');
    }
    sb.append(cents);
    return sb.toString();
  }

  public String getName() {
    return name;
  }

  public String getNotes() {
    return notes;
  }

  public int getPrice() {
    return price;
  }
  
  public String getTicker() {
    return ticker;
  }

  public boolean isFavorite() {
    return favorite;
  }

  public void setFavorite(boolean favorite) {
    this.favorite = favorite;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @Override
  public String toString() {
    return "StockQuote [ticker=" + ticker + ", name=\"" + name + "\", price="
        + price + ", notes=\"" + notes + "\", favorite=" + favorite + "]";
  }
}
