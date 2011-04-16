/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package burpmachinekeyutils;

import java.io.*;

public class LittleEndianOutputStream extends FilterOutputStream {

    protected int written;

    public LittleEndianOutputStream(OutputStream out) {
        super(out);
    }

    public void write(int b) throws IOException {
        out.write(b);
        written++;
    }

    public void write(byte[] data, int offset, int length)
            throws IOException {
        out.write(data, offset, length);
        written += length;
    }

    public void writeBoolean(boolean b) throws IOException {
        if (b) {
            this.write(1);
        } else {
            this.write(0);
        }
    }

    public void writeByte(int b) throws IOException {
        out.write(b);
        written++;
    }

    public void writeShort(int s) throws IOException {
        out.write(s & 0xFF);
        out.write((s >>> 8) & 0xFF);
        written += 2;
    }

    public void writeChar(int c) throws IOException {
        out.write(c & 0xFF);
        out.write((c >>> 8) & 0xFF);
        written += 2;
    }

    public void writeInt(int i) throws IOException {

        out.write(i & 0xFF);
        out.write((i >>> 8) & 0xFF);
        out.write((i >>> 16) & 0xFF);
        out.write((i >>> 24) & 0xFF);
        written += 4;

    }

    public void writeLong(long l) throws IOException {

        out.write((int) l & 0xFF);
        out.write((int) (l >>> 8) & 0xFF);
        out.write((int) (l >>> 16) & 0xFF);
        out.write((int) (l >>> 24) & 0xFF);
        out.write((int) (l >>> 32) & 0xFF);
        out.write((int) (l >>> 40) & 0xFF);
        out.write((int) (l >>> 48) & 0xFF);
        out.write((int) (l >>> 56) & 0xFF);
        written += 8;

    }

    public final void writeFloat(float f) throws IOException {
        this.writeInt(Float.floatToIntBits(f));
    }

    public final void writeDouble(double d) throws IOException {
        this.writeLong(Double.doubleToLongBits(d));
    }

    public void writeBytes(String s) throws IOException {
        int length = s.length();
        for (int i = 0; i < length; i++) {
            out.write((byte) s.charAt(i));
        }
        written += length;
    }
    public void writeBytes(byte[] s) throws IOException {
        int length = s.length;
        for (int i = 0; i < length; i++) {
            out.write((byte) s[i]);
        }
        written += length;
    }

    public void writeChars(String s) throws IOException {
        int length = s.length();
        for (int i = 0; i < length; i++) {
            int c = s.charAt(i);
            out.write(c & 0xFF);
            out.write((c >>> 8) & 0xFF);
        }
        written += length * 2;
    }

    public void writeUTF(String s) throws IOException {

        

    }

    public int size() {
        return this.written;
    }

    
}