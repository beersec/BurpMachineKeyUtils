/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package burpmachinekeyutils;

/**
 *
 * @author jparish
 */
public class HexUtils {

    /**
     * Converts a ASCII Hexadecimal string to an array of bytes
     * @param s ASCII Hexadecimal string
     * @return Array of bytes
     */
    public static byte[] toByteArray(String s) {
        s = s.toUpperCase();
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Converts a byte array into an ASCII hexadecimal string
     * @param b byte array
     * @return ASCII hexadecmial string
     */
    public static String toString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result +=
                    Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            if((i+1) % 8  == 0) result += "";
        }
        return result.toUpperCase();
    }
}
