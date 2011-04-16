/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package burpmachinekeyutils;

import java.io.*;

public class LittleEndianInputStream extends FilterInputStream {
    boolean debug;
    public LittleEndianInputStream(InputStream in, boolean d) {
        super(in);
        debug = d;
    }
    
    public boolean readBoolean() throws IOException {
        int bool = in.read();
        
        if (bool == -1) {
            throw new EOFException();
        }
        return (bool != 0);
    }

    public byte readByte() throws IOException {
        
        int temp = in.read();
        //if(debug) System.out.print(Integer.toString((temp & 0xff) + 0x100, 16).substring(1));
        //System.out.print(Integer.toString((temp & 0xff) + 0x100, 16).substring(1));
        if (temp == -1) {
            throw new EOFException();
        }
        return (byte) temp;
    }

    public int readUnsignedByte() throws IOException {
        int temp = in.read();
        if (temp == -1) {
            throw new EOFException();
        }
        return temp;
    }

    public short readShort() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();
        // only need to test last byte read
        // if byte1 is -1 so is byte2
        if (byte2 == -1) {
            throw new EOFException();
        }
        return (short) (((byte2 << 24) >>> 16) + (byte1 << 24) >>> 24);
    }

    public int readUnsignedShort() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();
        if (byte2 == -1) {
            throw new EOFException();
        }
        return ((byte2 << 24) >> 16) + ((byte1 << 24) >> 24);
    }

    public char readChar() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();
        if (byte2 == -1) {
            throw new EOFException();
        }
        return (char) (((byte2 << 24) >>> 16) + ((byte1 << 24) >>> 24));
    }

    public int readInt() throws IOException {

        int byte1 = in.read();
        int byte2 = in.read();
        int byte3 = in.read();
        int byte4 = in.read();
        if (byte4 == -1) {
            throw new EOFException();
        }
        return (byte4 << 24)
                + ((byte3 << 24) >>> 8)
                + ((byte2 << 24) >>> 16)
                + ((byte1 << 24) >>> 24);

    }

    public long readLong() throws IOException {

        long byte1 = readByte();
        long byte2 = readByte();
        long byte3 = readByte();
        long byte4 = readByte();
        long byte5 = readByte();
        long byte6 = readByte();
        long byte7 = readByte();
        long byte8 = readByte();
        if (byte8 == -1) {
            throw new EOFException();
        }
        return (byte8 << 56)
                + ((byte7 << 56) >>> 8)
                + ((byte6 << 56) >>> 16)
                + ((byte5 << 56) >>> 24)
                + ((byte4 << 56) >>> 32)
                + ((byte3 << 56) >>> 40)
                + ((byte2 << 56) >>> 48)
                + ((byte1 << 56) >>> 56);

    }

    public String readUTF() throws IOException {

        String result = "";
        int temp;
        int last = 0;
        while (true) {

           temp = readByte();
           if(temp == 0 && last == 0) break;
           last = temp;
           result += (char) temp;
           

        }  

        return result;

    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    public final int skipBytes(int n) throws IOException {
        for (int i = 0; i < n; i += (int) skip(n - i));
        return n;
    }
}