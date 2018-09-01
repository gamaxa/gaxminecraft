package com.gamaxa.buycraft;

/**
 * @author PaulBGD
 */
public class BuycraftResponse extends BuycraftErrorResponse {
    public BuycraftData data;

    @Override
    public String toString() {
        return "BuycraftResponse{" +
                "data=" + data +
                "} " + super.toString();
    }

    public class BuycraftData {
        public int id;
        public String code;

        @Override
        public String toString() {
            return "BuycraftData{" +
                    "id=" + id +
                    ", code='" + code + '\'' +
                    '}';
        }
    }
}
