package com.circulation.universa_cell.utils;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.math.BigInteger;
import java.util.function.ToIntFunction;

public final class CellStorageMath {

    private CellStorageMath() {
    }

    public static <T> long usedBytes(
        Object2LongMap<T> amountsByKey,
        ToIntFunction<T> amountPerByte,
        long storedTypes,
        int bytesPerType) {
        long resourceBytes = 0;
        for (Object2LongMap.Entry<T> entry : amountsByKey.object2LongEntrySet()) {
            long amount = Math.max(0, entry.getLongValue());
            int multiplier = Math.max(1, amountPerByte.applyAsInt(entry.getKey()));
            resourceBytes = saturatedAdd(resourceBytes, ceilDiv(amount, multiplier));
        }
        return saturatedAdd(resourceBytes, saturatedMultiply(storedTypes, bytesPerType));
    }

    static long ceilDiv(long value, long divisor) {
        if (value <= 0) {
            return 0;
        }
        return 1 + (value - 1) / divisor;
    }

    public static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    public static long maxAmountPerType(long totalBytes, long totalTypes, int bytesPerType, int amountPerByte) {
        if (totalBytes <= 0 || totalTypes <= 0 || bytesPerType < 0 || amountPerByte <= 0) {
            return 0;
        }
        long resourceBytes = Math.max(0, totalBytes - saturatedMultiply(totalTypes, bytesPerType));
        BigInteger amount = BigInteger.valueOf(resourceBytes)
                .multiply(BigInteger.valueOf(amountPerByte))
                .add(BigInteger.valueOf(totalTypes - 1))
                .divide(BigInteger.valueOf(totalTypes));
        return amount.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue();
    }

    static long saturatedMultiply(long left, long right) {
        if (left <= 0 || right <= 0) {
            return 0;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }
}
