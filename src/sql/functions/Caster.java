package sql.functions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Caster {

    @Contract(pure = true)
    public static int bytesToInt(@NotNull byte[] bytes) {
        int result = bytes[0] & 0xff;
        result = result << 8 | bytes[1] & 0xff;
        result = result << 8 | bytes[2] & 0xff;
        result = result << 8 | bytes[3] & 0xff;
        return result;
    }

    @NotNull
    @Contract(pure = true)
    public static byte[] intToBytes(int num) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (num >>> 24);
        bytes[1] = (byte) (num >>> 16);
        bytes[2] = (byte) (num >>> 8);
        bytes[3] = (byte) num;
        return bytes;
    }

    @NotNull
    @Contract(pure = true)
    public static int[] longToInt(long number) {
        int[] result = new int[2];
        result[0] = (int) (number >> 32);
        result[1] = (int) (number & 0xffffffffL);
        return result;
    }

    @Contract(pure = true)
    public static long intsToLong(@NotNull int[] ints) {
        return ((long) ints[0] << 32) + ints[1];
    }
}
