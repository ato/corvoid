package corvoid;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

class Json {
    static Object read(InputStream stream) throws IOException {
        return read(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
    }

    static Object read(Reader reader) throws IOException {
        return new Parser(reader).value();
    }

    private static class Parser {
        private final Reader reader;
        private int peek = -2;

        Parser(Reader reader) {
            this.reader = reader;
        }

        private int peek() throws IOException {
            if (peek == -2) peek = reader.read();
            return peek;
        }

        private int next() throws IOException {
            int c = peek();
            peek = -2;
            return c;
        }

        private int look() throws IOException {
            int c = peek();
            while (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                c = reader.read();
            }
            peek = c;
            return c;
        }

        private void consume(int c) throws IOException {
            if (next() != c) throw new IOException("Expected '" + (char) c + "'");
        }

        Object value() throws IOException {
            return switch (look()) {
                case '"' -> string();
                case '{' -> object();
                case '[' -> array();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> number();
                case -1 -> throw new EOFException();
                default -> throw new IOException("Unexpected character");
            };
        }

        private Object number() throws IOException {
            StringBuilder buffer = new StringBuilder();
            boolean dbl = false;
            while (true) {
                int c = peek();
                if (c == 'e' || c == 'E' || c == '.') {
                    dbl = true;
                } else if ((c < '0' || c > '9') && c != '-' && c != '+') {
                    try {
                        if (dbl) return Double.parseDouble(buffer.toString());
                        return Long.parseLong(buffer.toString());
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid number: " + buffer);
                    }
                }
                buffer.append((char) next());
            }
        }

        private Object literal(String s, Boolean value) throws IOException {
            for (int i = 0; i < s.length(); i++) {
                if (next() != s.charAt(i)) throw new IOException("Expected '" + s + "'");
            }
            return value;
        }

        private Object array() throws IOException {
            consume('[');
            Collection<Object> list = new ArrayList<>();
            if (look() != ']') {
                while (true) {
                    list.add(value());
                    if (look() == ']') break;
                    consume(',');
                }
            }
            consume(']');
            return list;
        }

        private Object object() throws IOException {
            consume('{');
            Map<String, Object> map = new LinkedHashMap<>();
            if (look() != '}') {
                while (true) {
                    String key = string();
                    consume(':');
                    map.put(key, value());
                    if (look() == '}') break;
                    consume(',');
                    look();
                }
            }
            consume('}');
            return map;
        }

        private String string() throws IOException {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = next();
                if (c < 0) throw new EOFException("Unterminated JSON string");
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append((char) c);
                    continue;
                }
                c = next();
                sb.append(switch (c) {
                    case '"', '\\', '/' -> (char) c;
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'u' -> {
                        int x = 0;
                        for (int i = 0; i < 4; i++) {
                            c = next();
                            int digit = Character.digit(c, 16);
                            if (digit < 0) throw new IOException("Invalid hex digit in unicode escape: " + (char) c);
                            x = (x << 4) + digit;
                        }
                        yield (char) x;
                    }
                    default -> throw new IOException("Invalid escape character: \\" + (char) c);
                });
            }
        }
    }

    static void write(OutputStream out, Object value) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        write(writer, value);
        writer.flush();
    }

    static void write(Appendable out, Object value) throws IOException {
        switch (value) {
            case null -> out.append("null");
            case Boolean b -> out.append(b.toString());
            case String s -> {
                out.append('"');
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    char escape = switch (c) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '\b' -> 'b';
                        case '\f' -> 'f';
                        case '\n' -> 'n';
                        case '\r' -> 'r';
                        case '\t' -> 't';
                        default -> 0;
                    };
                    if (escape != 0) {
                        out.append('\\');
                        out.append(escape);
                    } else if (c <= 0x1f) {
                        out.append("\\u00");
                        out.append(Character.forDigit((c & 0xf0) >>> 4, 16));
                        out.append(Character.forDigit(c & 0xf, 16));
                    } else {
                        out.append(c);
                    }
                }
                out.append('"');
            }
            case Number number -> out.append(number.toString());
            case Map<?,?> map -> {
                out.append('{');
                boolean first = true;
                for (var entry : map.entrySet()) {
                    if (!first) out.append(',');
                    first = false;
                    write(out, entry.getKey());
                    out.append(':');
                    write(out, entry.getValue());
                }
                out.append('}');
            }
            case Collection<?> coll -> {
                out.append('[');
                boolean first = true;
                for (Object o : coll) {
                    if (!first) out.append(',');
                    first = false;
                    write(out, o);
                }
                out.append(']');
            }
            default -> throw new IllegalArgumentException("unsupported JSON type: " + value.getClass());
        }
    }
}