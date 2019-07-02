/**
 *
 */
package com.easy.detection.output;

/**
 * Static selection of functions on Enumerations that model CSV files
 *
 * @author wfenske
 */
public class CsvEnumUtils {
    /*
     * not meant to be instantiated
     */
    private CsvEnumUtils() {
    }

    public static <TEnum extends Enum<?>> Object[] headerRow(Class<? extends TEnum> columnsClass) {
        TEnum[] enumConstants = columnsClass.getEnumConstants();
        if (enumConstants == null) throw new IllegalArgumentException("Not an enum type: " + columnsClass);
        final int numColumns = enumConstants.length;
        Object[] r = new Object[numColumns];
        for (int i = 0; i < numColumns; i++) {
            TEnum e = enumConstants[i];
            r[i] = e.name();
        }
        return r;
    }

    public static <TEnum extends Enum<?>> String[] headerRowStrings(Class<? extends TEnum> columnsClass) {
        Object[] names = headerRow(columnsClass);
        String[] result = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = names[i].toString();
        }
        return result;
    }

    public static <TEnum extends Enum<?>> void validateHeaderRow(Class<? extends TEnum> columnsClass, String[] headerLine) {
        TEnum[] enumConstants = columnsClass.getEnumConstants();
        if (enumConstants == null) throw new IllegalArgumentException("Not an enum type: " + columnsClass);
        final int minCols = enumConstants.length;

        if (headerLine.length < minCols) {
            throw new RuntimeException("Not enough columns. Expected at least " + minCols + ", got " + headerLine.length);
        }

        for (int col = 0; col < minCols; col++) {
            String expectedColName = enumConstants[col].name();
            if (!headerLine[col].equalsIgnoreCase(expectedColName)) {
                throw new RuntimeException("Column name mismatch. Expected column " + col + " to be " + expectedColName + ", got: " + headerLine[col]);
            }
        }
    }

    /**
     * Convert the given object, <code>o</code>, into a list of column values for serialization into a CSv file
     *
     * @param columnsClass
     * @param o
     * @param ctx
     * @param <TInput>
     * @param <TEnum>
     * @return An array of objects, one for each column of the resulting CSV file
     */
    public static <TInput, TContext, TEnum extends Enum<?> & CsvColumnValueProvider<TInput, TContext>> Object[] dataRow(
            Class<? extends TEnum> columnsClass, TInput o, TContext ctx) {
        TEnum[] enumConstants = columnsClass.getEnumConstants();
        if (enumConstants == null) throw new IllegalArgumentException("Not an enum type: " + columnsClass);
        final int len = enumConstants.length;
        Object[] r = new Object[len];
        for (int i = 0; i < len; i++) {
            TEnum e = enumConstants[i];
            Object value = e.csvColumnValue(o, ctx);
            r[i] = value;
        }
        return r;
    }
}
