package burpmachinekeyutils;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


import burp.StartBurp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 
 * @author jparish
 */
public class BurpMachineKeyUtils {

    private static String decryptionKey;
    private static String validationKey;
    
    /**
     * Set the decryption key for encrypting/decrypting
     * @param DecryptionKey
     */
    public static void setDecryptionKey(String DecryptionKey) {
        decryptionKey = DecryptionKey;
    }

    /**
     * Set the validation key for signing
     * @param ValidationKey
     */
    public static void setValidationKey(String ValidationKey) {
        validationKey = ValidationKey;
    }
    
    private static byte[] transform(byte[] encodedData, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

        Security.addProvider(new BouncyCastleProvider());

        SecretKeySpec key = new SecretKeySpec(HexUtils.toByteArray(decryptionKey), "AES");
        IvParameterSpec spec = new IvParameterSpec(new byte[16]);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");


        try {
            cipher.init(mode, key, spec);
            return cipher.doFinal(encodedData);

        } catch (Exception ex) {

            return null;
        }

    }

   

    /**
     * Decrypt the ticket bytes with the decryption key then verify the signature with HMACSHA1 and the validation key
     * @param encryptedBytes encrypted bytes of FormsAuthentication token
     * @return decrypted bytes of the ticket or null if the bytes do not match the tamper signature
     */
    public byte[] DecryptAndVerify(byte[] encryptedBytes) {
        
        try {
            byte[] decryptedData = transform(encryptedBytes, Cipher.DECRYPT_MODE);
            
            byte[] unsignedData = new byte[decryptedData.length - 20];
            System.arraycopy(decryptedData, 0, unsignedData, 0, decryptedData.length - 20);
            byte[] signature = HMACSHA1(unsignedData);

            boolean valid = true;
            for (int i = 0; i < 20; i++) {
                if (signature[i] != decryptedData[decryptedData.length - 20 + i]) {
                    valid = false;
                }
            }
            
            return valid ? unsignedData : null;
            
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * HMACSHA1 sign the bytes of the ticket with the validation key and encrypt with the decryption key
     * @param decryptedBytes ticket bytes
     * @return signed then encrypted bytes
     */
    public byte[] SignAndEncrypt(byte[] decryptedBytes) {
        byte[] signature = HMACSHA1(decryptedBytes);
        byte[] signed = new byte[decryptedBytes.length + 20];
        System.arraycopy(decryptedBytes, 0, signed, 0, decryptedBytes.length);
        System.arraycopy(signature, 0, signed, decryptedBytes.length, 20);
        try {
            return transform(signed, Cipher.ENCRYPT_MODE);
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] HMACSHA1(byte[] data) {
        try {
            

            SecretKey key = new SecretKeySpec(HexUtils.toByteArray(validationKey), "HmacSHA1");

            Mac m = Mac.getInstance("HmacSHA1");
            m.init(key);
            return m.doFinal(data);

        } catch (InvalidKeyException ex) {
            return null;
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
    public static void main(String[] args) {
        BurpMachineKeyUtils u = new BurpMachineKeyUtils();
        u.setValidationKey("EE0518F0C19F3F08D4215AA2CD913110C6945795A4473B141F71485F5BF6CE3F0D0E0E713FF3E4597DE5FAA5B8A5B10868A36007F91674AB70697F42E40852BB");
        u.setDecryptionKey("25635137BC21EF73F03038D4B70FF316D3D5372C4F64C2098297AAB5FC01722F");
        //byte[] cookie = HexUtils.toByteArray("0C8C19F9763843BE8314743ECE5DD6534AE1159903E21F4A14405A7D22F1D12B3C84216A35266A43949268AC5B35C1DCE5C8A7F0C2E04AC0858181D3CA6B06ADB412E16BAF441233F35A8A55A0BBCE9E9F42B690E8841E2AE73DAFB52DAC0791");
        byte[] cookie = HexUtils.toByteArray("E898432879F1CF14335BB44B962A7BEBD509D886B12BF27086D7409FA1BE1BC8891A776D6D8258C47E12D3947D1463DB2F99041788583052168B34E2AF25355F344F7A6E2B76F7404536899C52B228761DCCEC2DCB9E9B889CF29BC96FD2239F");
        byte[] viewstate = Base64Utils.decode("wEPDwUKMTUwMjA2OTIzM2QYAQUeX19Db250cm9sc1JlcXVpcmVQb3N0QmFja0tleV9fFgEFD2Noa1BlcnNpc3RMb2dpbhdlzUMb3qwQ0u9p+1ab5crJTyQW");
        System.out.println(new String());
        
        
         
       
        
        
    }
}
