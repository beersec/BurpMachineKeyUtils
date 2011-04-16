/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package burpmachinekeyutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 *
 * @author jparish
 */
public class FormsAuthenticationTicket {
    
    public int version = 2;    
    public boolean persistent = false;    
    public Date issueDate = new Date("1/28/2011 6:30:27 AM");   
    public Date expireDate = new Date("1/28/2011 11:10:27 PM");    
    public String username = "";    
    public String cookiePath = "/";
    public String userData = "";

    public static String encode(String str) {
        // Encode data in simple UTF-8 format 
        // test becomes {'t',0x00,'e',0x00,'s',0x00,'t',0x00,0x00}
        // TODO: support Unicode 
        if(str == null) str = "";
        if("".equals(str)) return new String();
        try {
            StringBuilder out = new StringBuilder();
            byte[] strBytes = str.getBytes("UTF-16");
            for (int i = 3; i < strBytes.length; i++) {
                out.append((char) strBytes[i]);
            }
            out.append((char) 0);
            
            return out.toString();
        } catch (UnsupportedEncodingException ex) {
            return null;
        }

    }
    
    /**
     * Convert a FormsAuthenticationTicket to the binary representation, see 
     * http://www.codeproject.com/KB/aspnet/Forms_Auth_Internals.aspx for 
     * details on the fromat.
     * @param ticket
     * @return binary representation of the ticket
     */
    public static byte[] toBytes(FormsAuthenticationTicket ticket) {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            LittleEndianOutputStream writer = new LittleEndianOutputStream(bs);
            // Random buffer to generate unique tokens even if they have
            // identical contents due to the way CBC chains
            writer.writeLong(4092844371316334320L); // Here 0 because we really don't care about this right now
            writer.writeByte(ticket.version);
            writer.writeBytes(encode(ticket.username));
            writer.writeBytes(new byte[]{0,0}); 
            writer.writeLong(FormsAuthenticationTicket.TimeToTicks(ticket.issueDate)); // Convert java time to windows file time
            writer.writeByte(ticket.persistent ? 1 : 0);
            writer.writeLong(FormsAuthenticationTicket.TimeToTicks(ticket.expireDate));
            writer.writeBytes(encode(ticket.userData));
            writer.writeBytes(new byte[]{0, 0});
            writer.writeBytes(encode(ticket.cookiePath));
            writer.writeBytes(new byte[]{0, 0});
            return bs.toByteArray();

        } catch (IOException ex) {

            return null;
        }


    }

    /**
     * Convert a binary ticket blob into a FormsAuthenticationTicket object.
     * See toBytes above and http://www.codeproject.com/KB/aspnet/Forms_Auth_Internals.aspx
     * @param ticketBytes Binary ticket blob
     * @return FormsAuthenticationTicket object
     */
    public static FormsAuthenticationTicket fromBytes(byte[] ticketBytes) {
        try {
            ByteArrayInputStream bs = new ByteArrayInputStream(ticketBytes);

            LittleEndianInputStream reader = new LittleEndianInputStream(bs, false);
            FormsAuthenticationTicket ticket = new FormsAuthenticationTicket();
            
            reader.readLong(); // Skip the random buffer
            ticket.version = reader.readByte(); 
            ticket.username = reader.readUTF();
            reader.readByte();
            ticket.issueDate = TicksToTime(reader.readLong());
            ticket.persistent = reader.readByte() == 1;
            ticket.expireDate = TicksToTime(reader.readLong());
            ticket.userData = reader.readUTF();
            reader.readByte();
            ticket.cookiePath = reader.readUTF();
            
            return ticket;
        } catch (IOException ex) {
            return null;
        }

    }
    public static void debug(byte[] ticketBytes) {
        try {
            ByteArrayInputStream bs = new ByteArrayInputStream(ticketBytes);

            LittleEndianInputStream reader = new LittleEndianInputStream(bs, true);
            FormsAuthenticationTicket ticket = new FormsAuthenticationTicket();
            
            System.out.println("Random: ");
            System.out.println(" " + reader.readLong()); // Skip the random buffer
            System.out.println("\nVersion: ");
            ticket.version = reader.readByte(); 
            System.out.println("\nUsername: ");
            ticket.username = reader.readUTF();
            reader.readByte();
            
            System.out.println("\nIssue: ");
            System.out.println(" " + reader.readLong());
            System.out.println("\nPersist: ");
            ticket.persistent = reader.readByte() == 1;
            System.out.println("\nExpire: ");
            System.out.println(" " + reader.readLong());
            System.out.println("\nData: ");
            ticket.userData = reader.readUTF();
            reader.readByte();
            System.out.println("\nPath: ");
            ticket.cookiePath = reader.readUTF();
            
            
            
        } catch (IOException ex) {
            
        }

    }

    public static long TimeToTicks(Date date) {
        
        return 10000 * (date.getTime() + 11644473600000L);
    }
    
    public static Date TicksToTime(long ticks) {
        
        return new Date((ticks / 10000) - 11644473600000L);
    }

    
}
