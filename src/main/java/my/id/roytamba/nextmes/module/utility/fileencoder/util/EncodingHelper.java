package my.id.roytamba.nextmes.module.utility.fileencoder.util;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;

public class EncodingHelper {

    // --- Base64 ---
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data);
    }

    // --- Hexadecimal (Base16) ---
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String encodeHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static byte[] decodeHex(String s) {
        String cleanString = s.replaceAll("\\s+", "").replaceAll("^0x", "").toUpperCase();
        int len = cleanString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanString.charAt(i), 16) << 4)
                                 + Character.digit(cleanString.charAt(i+1), 16));
        }
        return data;
    }

    // --- Base32 (RFC 4648) ---
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    public static String encodeBase32(byte[] bytes) {
        int i = 0, index = 0, digit = 0;
        int currByte, nextByte;
        StringBuilder base32 = new StringBuilder((bytes.length + 7) * 8 / 5);
        while (i < bytes.length) {
            currByte = (bytes[i] >= 0) ? bytes[i] : (bytes[i] + 256); // unsign
            if (index > 3) {
                if ((i + 1) < bytes.length) {
                    nextByte = (bytes[i + 1] >= 0) ? bytes[i + 1] : (bytes[i + 1] + 256);
                } else {
                    nextByte = 0;
                }
                digit = currByte & (0xFF >> index);
                index = (index + 5) % 8;
                digit <<= index;
                digit |= nextByte >> (8 - index);
                i++;
            } else {
                digit = (currByte >> (8 - (index + 5))) & 0x1F;
                index = (index + 5) % 8;
                if (index == 0) i++;
            }
            base32.append(BASE32_CHARS.charAt(digit));
        }
        return base32.toString();
    }
    public static byte[] decodeBase32(String base32) {
        base32 = base32.replaceAll("\\s+", "").toUpperCase().replaceAll("=", "");
        int i = 0, index = 0, lookup, offset = 0, digit;
        byte[] bytes = new byte[base32.length() * 5 / 8];
        for (i = 0; i < base32.length(); i++) {
            lookup = base32.charAt(i) - '0';
            if (lookup < 0 || lookup >= 43) continue;
            digit = BASE32_CHARS.indexOf(base32.charAt(i));
            if (digit == -1) continue;
            if (index <= 3) {
                index = (index + 5) % 8;
                if (index == 0) {
                    bytes[offset] |= digit;
                    offset++;
                    if (offset >= bytes.length) break;
                } else {
                    bytes[offset] |= digit << (8 - index);
                }
            } else {
                index = (index + 5) % 8;
                bytes[offset] |= (digit >>> index);
                offset++;
                if (offset >= bytes.length) break;
                bytes[offset] |= digit << (8 - index);
            }
        }
        return bytes;
    }

    // --- Base58 (Bitcoin standard) ---
    private static final char[] ALPHABET_B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    public static String encodeBase58(byte[] input) {
        if (input.length == 0) return "";
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            zeros++;
        }
        byte[] inputCopy = Arrays.copyOf(input, input.length);
        char[] encoded = new char[inputCopy.length * 2];
        int outputStart = encoded.length;
        for (int inputStart = zeros; inputStart < inputCopy.length; ) {
            encoded[--outputStart] = ALPHABET_B58[divmod(inputCopy, inputStart, 256, 58)];
            if (inputCopy[inputStart] == 0) {
                inputStart++; // skip leading zeros
            }
        }
        while (outputStart < encoded.length && encoded[outputStart] == ALPHABET_B58[0]) {
            outputStart++;
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ALPHABET_B58[0];
        }
        return new String(encoded, outputStart, encoded.length - outputStart);
    }
    
    private static byte divmod(byte[] number, int firstDigit, int base, int divisor) {
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = (int) number[i] & 0xFF;
            int temp = remainder * base + digit;
            number[i] = (byte) (temp / divisor);
            remainder = temp % divisor;
        }
        return (byte) remainder;
    }
    
    public static byte[] decodeBase58(String input) {
        if (input.length() == 0) return new byte[0];
        byte[] input58 = new byte[input.length()];
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int digit = -1;
            for (int j = 0; j < ALPHABET_B58.length; j++) {
                if (ALPHABET_B58[j] == c) {
                    digit = j;
                    break;
                }
            }
            if (digit < 0) throw new IllegalArgumentException("Invalid Base58 character: " + c);
            input58[i] = (byte) digit;
        }
        int zeros = 0;
        while (zeros < input58.length && input58[zeros] == 0) zeros++;
        byte[] decoded = new byte[input.length()];
        int outputStart = decoded.length;
        for (int inputStart = zeros; inputStart < input58.length; ) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256);
            if (input58[inputStart] == 0) inputStart++;
        }
        while (outputStart < decoded.length && decoded[outputStart] == 0) outputStart++;
        byte[] result = new byte[decoded.length - outputStart + zeros];
        System.arraycopy(decoded, outputStart, result, zeros, decoded.length - outputStart);
        return result;
    }

    // --- Ascii85 (Base85 / IPv6 standard Z85 style or Adobe style) ---
    // Using a simple Adobe Ascii85 implementation
    public static String encodeAscii85(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long tuple = 0;
        int count = 0;
        for (byte b : data) {
            tuple = (tuple << 8) | (b & 0xFF);
            count++;
            if (count == 4) {
                if (tuple == 0) {
                    out.write('z');
                } else {
                    encodeAscii85Tuple(tuple, 5, out);
                }
                tuple = 0;
                count = 0;
            }
        }
        if (count > 0) {
            int padding = 4 - count;
            tuple <<= (8 * padding);
            encodeAscii85Tuple(tuple, count + 1, out);
        }
        return "<~" + out.toString() + "~>";
    }

    private static void encodeAscii85Tuple(long tuple, int bytes, ByteArrayOutputStream out) {
        long[] pow85 = {85L*85*85*85, 85L*85*85, 85L*85, 85L, 1L};
        for (int i = 0; i < bytes; i++) {
            out.write((int)((tuple / pow85[i]) % 85) + 33);
        }
    }

    public static byte[] decodeAscii85(String ascii85) {
        String clean = ascii85.replaceAll("\\s+", "");
        if (clean.startsWith("<~")) clean = clean.substring(2);
        if (clean.endsWith("~>")) clean = clean.substring(0, clean.length() - 2);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long tuple = 0;
        int count = 0;
        
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == 'z' && count == 0) {
                out.write(0); out.write(0); out.write(0); out.write(0);
            } else if (c >= '!' && c <= 'u') {
                tuple = tuple * 85 + (c - 33);
                count++;
                if (count == 5) {
                    out.write((int)(tuple >> 24));
                    out.write((int)(tuple >> 16));
                    out.write((int)(tuple >> 8));
                    out.write((int)tuple);
                    tuple = 0;
                    count = 0;
                }
            } else {
                throw new IllegalArgumentException("Invalid Ascii85 character: " + c);
            }
        }
        if (count > 0) {
            for (int i = count; i < 5; i++) {
                tuple = tuple * 85 + 84;
            }
            if (count > 1) out.write((int)(tuple >> 24));
            if (count > 2) out.write((int)(tuple >> 16));
            if (count > 3) out.write((int)(tuple >> 8));
        }
        return out.toByteArray();
    }
}
