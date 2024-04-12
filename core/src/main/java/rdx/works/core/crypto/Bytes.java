package rdx.works.core.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

class Bytes {
    /**
     * An empty array of bytes.
     */
    public static final byte[] EMPTY_BYTES = new byte[0];
    private static final char[] hexChars = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private Bytes() {
        throw new IllegalStateException("Can't construct");
    }

    public static boolean arrayEquals(
            byte[] a1, int offset1, int length1, byte[] a2, int offset2, int length2) {
        if (length1 != length2) {
            return false;
        }
        for (int i = 0; i < length1; ++i) {
            if (a1[offset1 + i] != a2[offset2 + i]) {
                return false;
            }
        }
        return true;
    }

    public static int hashCode(byte[] a, int offset, int length) {
        int i = length;
        int hc = i + 1;
        while (--i >= 0) {
            hc *= 257;
            hc ^= a[offset + i];
        }
        return hc;
    }

    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Convert a byte into a two-digit hex string.
     *
     * <p>Note that digits a-f are output as lower case.
     *
     * @param b The byte to convert
     * @return The converted string
     */
    public static String toHexString(byte b) {
        char[] value = {toHexChar(b >> 4), toHexChar(b)};
        return new String(value);
    }

    /**
     * Convert an array into a string of hex digits.
     *
     * <p>The output string will have length {@code 2*bytes.length}. Hex digits a-f are encoded as
     * lower case.
     *
     * @param bytes The bytes to convert
     * @return The converted string
     */
    public static String toHexString(byte[] bytes) {
        return toHexString(bytes, 0, bytes.length);
    }

    /**
     * Convert a portion of an array into a string of hex digits.
     *
     * <p>The output string will have length {@code 2*length}. Hex digits a-f are encoded as lower
     * case.
     *
     * @param bytes  The bytes to convert
     * @param offset The offset at which to start converting
     * @param length The number of bytes to convert
     * @return The converted string
     */
    public static String toHexString(byte[] bytes, int offset, int length) {
        char[] chars = new char[length * 2];
        for (int i = 0; i < length; ++i) {
            byte b = bytes[offset + i];
            chars[i * 2] = hexChars[(b >> 4) & 0xF];
            chars[i * 2 + 1] = hexChars[b & 0xF];
        }
        return new String(chars);
    }

    /**
     * Convert a string of hexadecimal digits to an array of bytes.
     *
     * <p>If the string length is odd, a leading '0' is assumed.
     *
     * @param s The string to convert to a byte array.
     * @return The byte array corresponding to the converted string
     * @throws IllegalArgumentException if any character in s is not a hex digit
     */
    public static byte[] fromHexString(String s) {
        int byteCount = (s.length() + 1) / 2;
        byte[] bytes = new byte[byteCount];
        int index = 0;
        int offset = 0;
        // If an odd number of chars, assume leading zero
        if ((s.length() & 1) != 0) {
            bytes[offset++] = fromHexNybble(s.charAt(index++));
        }
        while (index < s.length()) {
            byte msn = fromHexNybble(s.charAt(index++));
            byte lsn = fromHexNybble(s.charAt(index++));
            bytes[offset++] = (byte) (((msn & 0xFF) << 4) | (lsn & 0xFF));
        }
        return bytes;
    }

    /**
     * Convert a base-64 encoded string into an array of bytes using RFC 4648 rules.
     *
     * @param s The string to convert
     * @return The decoded bytes
     */
    public static byte[] fromBase64String(String s) {
        return Base64.getDecoder().decode(s);
    }

    private static char toHexChar(int value) {
        return hexChars[value & 0xF];
    }

    private static byte fromHexNybble(char value) {
        char c = Character.toLowerCase(value);
        if (c >= '0' && c <= '9') {
            return (byte) (c - '0');
        }
        if (c >= 'a' && c <= 'f') {
            return (byte) (10 + c - 'a');
        }
        throw new IllegalArgumentException("Unknown hex digit: " + value);
    }

    /**
     * Trims any leading zero bytes from {@code bytes} until either no leading zero exists, or only a
     * single zero byte exists.
     *
     * @param bytes the byte a
     * @return @code bytes} with leading zeros removed, if any
     */
    public static byte[] trimLeadingZeros(byte[] bytes) {
        if (bytes == null || bytes.length <= 1 || bytes[0] != 0) {
            return bytes;
        }
        int trimLeadingZeros = 1;
        int maxTrim = bytes.length - 1;
        while (trimLeadingZeros < maxTrim && bytes[trimLeadingZeros] == 0) {
            trimLeadingZeros += 1;
        }
        return Arrays.copyOfRange(bytes, trimLeadingZeros, bytes.length);
    }

    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        final byte[] bytes = new byte[numBytes];
        final byte[] biBytes = b.toByteArray();
        final int start = biBytes.length == numBytes + 1 ? 1 : 0;
        final int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    public static BigInteger bytesToBigInteger(byte[] bytes) {
        return new BigInteger(1, bytes);
    }

    public static byte[] xor(byte[] a, byte[] b) {
        final byte[] ret = new byte[a.length];
        int i = 0;
        while (i < a.length) {
            ret[i] = (byte) (a[i] ^ b[i]);
            i += 1;
        }
        return ret;
    }

    /**
     * Checks whether a given array consists of only zero bytes.
     */
    public static boolean isAllZeros(byte[] arr) {
        for (byte b : arr) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
