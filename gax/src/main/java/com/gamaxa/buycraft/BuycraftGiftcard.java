package com.gamaxa.buycraft;

import java.io.Serializable;

/**
 * @author PaulBGD
 */
public class BuycraftGiftcard implements Serializable  {
    public BuycraftGiftcard(int id, String code) {
        this.id = id;
        this.code = code;
    }

    public int id;
    public String code;

}
