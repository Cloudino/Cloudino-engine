package io.cloudino.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;

/**
 *
 * @author serch
 */
public class TokenGenerator {

    private static SecureRandom generator = new SecureRandom();

    /**
     * Returns a Random token, it should have almost no collisions, but always
     * test for it
     *
     * @return a String token
     */
    public static String nextToken() {
        byte[] data = new byte[25];
        generator.nextBytes(data);
        return (new BigInteger(1, data)).toString(Character.MAX_RADIX);
    }
    
    public static String nextShortToken() {
        byte[] data = new byte[10];
        generator.nextBytes(data);
        return (new BigInteger(1, data)).toString(Character.MAX_RADIX);
    }

    public static String nextTokenByUserId(String userId) {
        BigInteger userInt = new BigInteger(userId, 16);
        byte[] data = new byte[25];
        byte[] tmpRnd = new byte[12];
        byte[] tmp = userInt.toByteArray();
        generator.nextBytes(tmpRnd);
        System.arraycopy(tmpRnd, 0, data, 0, 12);
        System.arraycopy(tmp, 0, data, 25 - tmp.length, tmp.length);
        return (new BigInteger(1, data)).toString(Character.MAX_RADIX);
    }

    public static String getUserIdFromToken(String token) {
        BigInteger tokenInt = new BigInteger(token, Character.MAX_RADIX);
        byte[] data = new byte[13];
        byte[] tmp = tokenInt.toByteArray();
        System.arraycopy(tmp, tmp.length - 13, data, 0, 13);
        return (new BigInteger(data)).toString(16);
    }

    public static String getNonExistentTokenByUserId(String userId, SWBDataSource ds) {
        try {
            String token = null;
            DataObject query = new DataObject();
            DataObject data = new DataObject();
            DataObject resp = null;
            do {
                token = nextTokenByUserId(userId);
                query.put("data", data);
                data.put("authToken", token);
                resp = ds.fetch(query);
            } while (resp.getDataObject("response").getDataList("data").size() > 0);
            return token;
        } catch (IOException ioe) {
            return null;
        }
    }
}
