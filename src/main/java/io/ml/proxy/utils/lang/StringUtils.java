package io.ml.proxy.utils.lang;

public class StringUtils {
    public static boolean isNotBlack(String str) {
        return !isBlack(str);
    }
    public static boolean isBlack(String str) {
        return str == null || str.isEmpty() || str.trim().isEmpty();
    }
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    public static String abbreviate(String str, int maxWidth) {
        return abbreviate(str, "...", 0, maxWidth);
    }

    public static String abbreviate(String str, int offset, int maxWidth) {
        return abbreviate(str, "...", offset, maxWidth);
    }

    public static String abbreviate(String str, String abbrevMarker, int maxWidth) {
        return abbreviate(str, abbrevMarker, 0, maxWidth);
    }

    public static String abbreviate(String str, String abbrevMarker, int offset, int maxWidth) {
        if (isNotEmpty(str) && "".equals(abbrevMarker) && maxWidth > 0) {
            return substring(str, 0, maxWidth);
        }/* else if (isAnyEmpty(str, abbrevMarker)) {
            return str;
        }*/ else {
            int abbrevMarkerLength = abbrevMarker.length();
            int minAbbrevWidth = abbrevMarkerLength + 1;
            int minAbbrevWidthOffset = abbrevMarkerLength + abbrevMarkerLength + 1;
            if (maxWidth < minAbbrevWidth) {
                throw new IllegalArgumentException(String.format("Minimum abbreviation width is %d", minAbbrevWidth));
            } else if (str.length() <= maxWidth) {
                return str;
            } else {
                if (offset > str.length()) {
                    offset = str.length();
                }

                if (str.length() - offset < maxWidth - abbrevMarkerLength) {
                    offset = str.length() - (maxWidth - abbrevMarkerLength);
                }

                if (offset <= abbrevMarkerLength + 1) {
                    return str.substring(0, maxWidth - abbrevMarkerLength) + abbrevMarker;
                } else if (maxWidth < minAbbrevWidthOffset) {
                    throw new IllegalArgumentException(String.format("Minimum abbreviation width with offset is %d", minAbbrevWidthOffset));
                } else {
                    return offset + maxWidth - abbrevMarkerLength < str.length() ? abbrevMarker + abbreviate(str.substring(offset), abbrevMarker, maxWidth - abbrevMarkerLength) : abbrevMarker + str.substring(str.length() - (maxWidth - abbrevMarkerLength));
                }
            }
        }
    }

    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        } else {
            if (end < 0) {
                end += str.length();
            }

            if (start < 0) {
                start += str.length();
            }

            if (end > str.length()) {
                end = str.length();
            }

            if (start > end) {
                return "";
            } else {
                if (start < 0) {
                    start = 0;
                }

                if (end < 0) {
                    end = 0;
                }

                return str.substring(start, end);
            }
        }
    }
}
