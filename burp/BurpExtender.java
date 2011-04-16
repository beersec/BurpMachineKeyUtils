/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package burp;

import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.util.List;
import burpmachinekeyutils.*;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.*;
import javax.xml.parsers.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class BurpExtender {

    private static String FormsCookieName = "AuthCookie";
    private BurpMachineKeyUtils machinekeyUtils = new BurpMachineKeyUtils();
    private static boolean active = false;
    public void setCommandLineArgs(String[] args) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            // Grab keys from Web.config

            Document doc = builder.parse(new FileInputStream(args[0]));
            NamedNodeMap machineKeySection = doc.getElementsByTagName("machineKey").item(0).getAttributes();
            machinekeyUtils.setValidationKey(machineKeySection.getNamedItem("validationKey").getNodeValue());
            machinekeyUtils.setDecryptionKey(machineKeySection.getNamedItem("decryptionKey").getNodeValue());
            NamedNodeMap formsSection = doc.getElementsByTagName("forms").item(0).getAttributes();
            FormsCookieName = formsSection.getNamedItem("name").getNodeValue();
            System.out.println("[+] Successfully imported machine keys "+ args[0]);
            
            active = true;
        } catch (Exception ex) {
            System.err.println("[!] Could not set machine keys from file.");
            
            
        
            

        }
    }
    
    private static String stripControls(String input) {
        // only allow printable characters in HTTP headers
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c > 0x20 && c < 0x7f) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String createCookieHeader(HttpCookie cookie) {

        String header = stripControls(cookie.getName()) + "=" + stripControls(cookie.getValue());

        if (cookie.getDomain() != null) {
            header += "; Domain=" + cookie.getDomain();
        }
        if (cookie.getPath() != null) {
            header += "; Path=" + cookie.getPath();
        }
        if (cookie.getSecure()) {
            header += "; Secure";
        }

        return header;
    }

    private static List<HttpCookie> grabResponseCookies(byte[] response) {
        String[] res = new String(response).split("\r\n\r\n")[0].split("\r\n");

        List<HttpCookie> cookies = null;
        for (String header : res) {
            if (header.startsWith("Set-Cookie")) {
                header.replaceAll("HttpOnly", "");

                cookies = HttpCookie.parse(header);

            }
        }

        return cookies;
    }

    private byte[] modifyRequest(byte[] request) {
        // merge __bMKU internal cookies into FormsAuthentication cookies, sign and encrypt them
        String[] res = new String(request).split("\r\n\r\n")[0].split("\r\n");

        boolean modified = false;
        for (String header : res) {
            if (header.startsWith("Cookie")) {

                HashMap<String, String> cookies = getRequestCookies(header.substring("Cookie:".length()));
               
                if (cookies.containsKey("__bMKUusername")) {
                    FormsAuthenticationTicket t = new FormsAuthenticationTicket();
                    t.username = cookies.get("__bMKUusername");
                    t.userData = cookies.get("__bMKUuserdata");
                    Calendar v = Calendar.getInstance();
                    v.add(Calendar.DAY_OF_MONTH, 2);
                    t.expireDate = v.getTime();
                    t.cookiePath = "/";

                    cookies.put(
                            FormsCookieName, HexUtils.toString(
                                machinekeyUtils.SignAndEncrypt(
                                    FormsAuthenticationTicket.toBytes(t)
                                )
                            )
                    );
                    
                   /* FormsAuthenticationTicket.debug(
                                    machinekeyUtils.DecryptAndVerify(
                                        HexUtils.toByteArray(cookies.get(FormsCookieName))
                                    )
                            );*/
                    
                    
                    cookies.remove("__bMKUusername");
                    cookies.remove("__bMKUuserdata");
                    modified = true;

                } 
                if (modified) {
                    request = replaceRequestCookies(request, cookies);
                    
                    return request;
                }

            }
        }

        return request;
    }

    public static HashMap<String, String> getRequestCookies(String cookieHeaderData) {

        String[] cookies = cookieHeaderData.split(";");
        HashMap<String, String> returnCookies = new HashMap<String, String>();
        for (int i = 0; i < cookies.length; i++) {
            String[] pair = cookies[i].trim().split("=",2);
            if (pair.length > 1) {
                returnCookies.put(pair[0], pair[1]);
            }

        }
        return returnCookies;
    }

    public void processHttpMessage(String toolName, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        try {
            if(active) {
                if (messageIsRequest ) {
                    //messageInfo.setRequest(modifyRequest(messageInfo.getRequest()));
                    messageInfo.setRequest(modifyRequest(messageInfo.getRequest()));
                    System.out.println(new String(modifyRequest(messageInfo.getRequest())).split("\r\n\r\n")[0]);
                    
                } else {
                    messageInfo.setResponse(modifyResponse(messageInfo.getResponse()));
                    //System.out.println(new String(messageInfo.getResponse()));
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private byte[] modifyResponse(byte[] response) {
        try {
            
            List<HttpCookie> cookies = grabResponseCookies(response);
            // only bother modifying cookies should we see a FormsAuthentication cookie
            boolean responseModified = false;
            if (cookies != null) {
                for (int i = 0; i < cookies.size(); i++) {

                    if (cookies.get(i).getName().equals(FormsCookieName)) {
                        responseModified = true;
                        // if the cookie is not null, split it out
                        if (cookies.get(i).getValue().length() > 0) {
                            // create ticket from signed and encrypted blob
                            System.out.println(cookies.get(i).getValue());
                            FormsAuthenticationTicket t = FormsAuthenticationTicket.fromBytes(
                                    machinekeyUtils.DecryptAndVerify(
                                        HexUtils.toByteArray(cookies.get(i).getValue())
                                    )
                            );
                            
                            cookies.add(new HttpCookie("__bMKUusername", java.net.URLEncoder.encode(stripControls(t.username), "UTF-8")));
                            cookies.add(new HttpCookie("__bMKUuserdata", java.net.URLEncoder.encode(stripControls(t.userData), "UTF-8")));

                            cookies.remove(i);
                            // otherwise reset it
                        } else {
                            cookies.add(new HttpCookie("__bMKUusername", ""));
                            cookies.add(new HttpCookie("__bMKUuserdata", ""));
                            cookies.remove(i);
                        }
                    }
                }
            }
            
            return responseModified ? replaceResponseCookies(response, cookies) : response;
           
        } catch (Exception ex) {           
            ex.printStackTrace();
            return null;
        }
    }

    private byte[] replaceResponseCookies(byte[] response, List<HttpCookie> cookies) {
        String res = new String(response);
        res.replaceFirst("Connection:", "nConection:");
        Pattern pattern = Pattern.compile("Set-Cookie:");
        Matcher matcher = pattern.matcher(res);
        //Change all Set-Cookie headers to fake http headers and insert our modified cookies
        if (matcher.find()) {
            res = matcher.replaceAll("Dont-Set-Cookie:");
            pattern = Pattern.compile("Dont-Set-Cookie:");
            matcher = pattern.matcher(res);
            StringBuilder newHeader = new StringBuilder();
            for (HttpCookie cookie : cookies) {
                newHeader.append("Set-Cookie: ");
                newHeader.append(createCookieHeader(cookie));
                newHeader.append("\r\n");
            }

            res = matcher.replaceAll(newHeader.toString() + "Dont-Set-Cookie:");

        }
        try {
            return res.getBytes("ASCII");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    private static byte[] replaceRequestCookies(byte[] request, HashMap<String, String> cookies) {

        String res = new String(request).replaceFirst("Connection:", "nConection:").replaceFirst("Keep-Alive:","Dont-Keep-Alive:") ;
        // Take Cookie <Name,Value> HashMap and convert to request header
        StringBuilder cookieBuilder = new StringBuilder("Cookie: ");

        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            cookieBuilder.append(cookie.getKey());
            cookieBuilder.append("=");
            cookieBuilder.append(cookie.getValue());
            cookieBuilder.append(";");
        }
        //cookieBuilder.append("abcdef=defg");
        cookieBuilder.append("");

        // match existing header
        Matcher m = Pattern.compile("Cookie:(.*)", Pattern.MULTILINE).matcher(res);

        
        

        try {
            //replace existing request header with new header
            return m.replaceFirst(cookieBuilder.toString()).getBytes("ASCII");
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }
    
}
