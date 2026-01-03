package corvoid;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Version implements Comparable<Version> {
    private static final List<String> QUALIFIERS = List.of("alpha", "beta", "milestone", "rc", "snapshot", "", "sp");
    private static final int STABLE_RANK = 5;
    private final String value;
    private final Object[] parts;

    public Version(String version) {
        this.value = version;
        this.parts = parse(version.toLowerCase(Locale.ENGLISH));
    }

    private static Object[] parse(String s) {
        List<Object> root = new ArrayList<>(), current = root;
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (isDelimiter(c)) {
                if (i == 0 || isDelimiter(s.charAt(i - 1))) {
                    current.add(BigInteger.ZERO);
                }
                if (c == '-' && !isHyphenTransition(s, i)) {
                    if (!current.isEmpty()) current = addSublist(current);
                }
                i++;
                continue;
            }
            int start = i;
            boolean isDigit = Character.isDigit(c);
            while (i < n && Character.isDigit(s.charAt(i)) == isDigit && !isDelimiter(s.charAt(i))) i++;
            String token = s.substring(start, i);
            if (isDigit) {
                current.add(new BigInteger(token));
            } else {
                i = handleStringToken(s, i, token, current);
            }
        }
        normalize(root);
        return toArray(root);
    }

    private static boolean isDelimiter(char c) {
        return c == '.' || c == '-';
    }

    private static boolean isHyphenTransition(String s, int i) {
        return i + 1 < s.length() && Character.isDigit(s.charAt(i + 1)) && i > 0 && Character.isLetter(s.charAt(i - 1));
    }

    private static int handleStringToken(String s, int i, String token, List<Object> current) {
        int n = s.length();
        int dStart = -1;
        if (i < n && Character.isDigit(s.charAt(i))) {
            dStart = i;
        } else if (i < n && s.charAt(i) == '-' && i + 1 < n && Character.isDigit(s.charAt(i + 1))) {
            dStart = i + 1;
            i++;
        }

        if (dStart != -1) {
            int dEnd = dStart;
            while (dEnd < n && Character.isDigit(s.charAt(dEnd))) dEnd++;
            BigInteger number = new BigInteger(s.substring(dStart, dEnd));
            if (!current.isEmpty()) {
                addSublist(current).add(new Combination(expand(token), number));
            } else {
                current.add(new Combination(expand(token), number));
            }
            return dEnd;
        } else {
            if (i == n && !current.isEmpty()) {
                addSublist(current).add(normalizeQualifier(token));
            } else {
                current.add(normalizeQualifier(token));
            }
            return i;
        }
    }

    private static List<Object> addSublist(List<Object> current) {
        List<Object> next = new ArrayList<>();
        current.add(next);
        return next;
    }

    private static String expand(String s) {
        return switch (s) {
            case "a" -> "alpha";
            case "b" -> "beta";
            case "m" -> "milestone";
            default -> normalizeQualifier(s);
        };
    }

    private static String normalizeQualifier(String s) {
        return switch (s) {
            case "cr" -> "rc";
            case "ga", "final", "release" -> "";
            default -> s;
        };
    }

    private static void normalize(List<?> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Object o = list.get(i);
            if (o instanceof List<?> sub) {
                normalize(sub);
            }
            if (shouldRemove(list, i)) {
                list.remove(i);
            }
        }
    }

    private static boolean shouldRemove(List<?> list, int i) {
        Object current = list.get(i);
        if (!isNull(current)) return false;
        if (i == list.size() - 1) return true;
        Object next = list.get(i + 1);
        return next instanceof String || (next instanceof List<?> l && !l.isEmpty() && (l.getFirst() instanceof String || l.getFirst() instanceof Combination));
    }

    private static boolean isNull(Object o) {
        return switch (o) {
            case BigInteger b -> b.signum() == 0;
            case String s -> s.isEmpty();
            case List<?> l -> l.isEmpty();
            default -> false;
        };
    }

    private static Object[] toArray(List<?> list) {
        Object[] arr = new Object[list.size()];
        for (int i = 0; i < arr.length; i++) {
            Object o = list.get(i);
            arr[i] = o instanceof List<?> l ? toArray(l) : o;
        }
        return arr;
    }

    private static int compare(Object o1, Object o2) {
        if (o1 == o2) return 0;
        if (o1 == null) return -compareWithNull(o2);
        if (o2 == null) return compareWithNull(o1);
        
        int t1 = type(o1), t2 = type(o2);
        if (t1 != t2) return Integer.compare(t1, t2);
        
        return switch (o1) {
            case BigInteger b1 -> b1.compareTo((BigInteger) o2);
            case Object[] a1 -> compareLists(a1, (Object[]) o2);
            case String s1 -> compareStrings(s1, o2);
            case Combination c1 -> compareCombination(c1, o2);
            default -> 0;
        };
    }

    private static int compareWithNull(Object o) {
        return switch (o) {
            case BigInteger b -> b.signum() == 0 ? 0 : 1;
            case String s -> Integer.compare(rank(s), STABLE_RANK);
            case Combination c -> Integer.compare(rank(c.s), STABLE_RANK);
            case Object[] a -> {
                for (Object part : a) {
                    int res = compareWithNull(part);
                    if (res != 0) yield res;
                }
                yield 0;
            }
            default -> 0;
        };
    }

    private static int compareStrings(String s1, Object o2) {
        if (o2 instanceof Combination c2) {
            int res = compareQualifiers(s1, c2.s);
            return res == 0 ? -1 : res;
        }
        return compareQualifiers(s1, (String) o2);
    }

    private static int compareCombination(Combination c1, Object o2) {
        String s2 = o2 instanceof Combination c2 ? c2.s : (String) o2;
        int res = compareQualifiers(c1.s, s2);
        if (res != 0) return res;
        return o2 instanceof Combination c2 ? c1.n.compareTo(c2.n) : 1;
    }

    private static int type(Object o) {
        return switch (o) {
            case BigInteger ignored -> 2;
            case Object[] ignored -> 1;
            default -> 0;
        };
    }

    private static int compareLists(Object[] a1, Object[] a2) {
        for (int i = 0; i < Math.max(a1.length, a2.length); i++) {
            int res = compare(i < a1.length ? a1[i] : null, i < a2.length ? a2[i] : null);
            if (res != 0) return res;
        }
        return 0;
    }

    private static int rank(String s) {
        int r = QUALIFIERS.indexOf(s);
        return r != -1 ? r : 7;
    }

    private static int compareQualifiers(String s1, String s2) {
        int r1 = rank(s1), r2 = rank(s2);
        return r1 != r2 ? Integer.compare(r1, r2) : (r1 == 7 ? s1.compareTo(s2) : 0);
    }

    @Override
    public int compareTo(Version other) {
        return compareLists(parts, other.parts);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Version && compareTo((Version) obj) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(parts);
    }

    public boolean isStable() {
        return !containsUnstableQualifier(parts);
    }

    private static boolean containsUnstableQualifier(Object obj) {
        return switch (obj) {
            case String s -> {
                int r = rank(s);
                yield r >= 0 && r < STABLE_RANK;
            }
            case Combination c -> {
                int r = rank(c.s);
                yield r >= 0 && r < STABLE_RANK;
            }
            case Object[] arr -> {
                for (Object part : arr) {
                    if (containsUnstableQualifier(part)) yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private record Combination(String s, BigInteger n) {
    }
}
