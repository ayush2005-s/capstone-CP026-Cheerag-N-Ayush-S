package com.nutriplanner.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary encoding for {@link Doc}. Layout: a format byte, the field count, then for every field its name, a type
 * tag and the value. Strings are length-prefixed UTF-8.
 */
public final class DocCodec {

    private static final byte FORMAT = 1;
    private static final byte NULL = 0, STRING = 1, LONG = 2, DOUBLE = 3, BOOL = 4, DATE = 5, INSTANT = 6, LIST = 7, DOC = 8;

    private DocCodec() {
    }

    public static byte[] encode(Doc doc) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(FORMAT);
            writeDoc(out, doc);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new StorageException("Cannot encode document", e);
        }
    }

    public static Doc decode(byte[] data) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            byte format = in.readByte();
            if (format != FORMAT) throw new StorageException("Unknown document format " + format);
            return readDoc(in);
        } catch (IOException e) {
            throw new StorageException("Corrupt document", e);
        }
    }

    private static void writeDoc(DataOutputStream out, Doc doc) throws IOException {
        Map<String, Object> fields = doc.fields();
        out.writeInt(fields.size());
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            writeString(out, e.getKey());
            writeValue(out, e.getValue());
        }
    }

    private static Doc readDoc(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > 100_000) throw new IOException("Bad field count " + count);
        Doc doc = new Doc();
        for (int i = 0; i < count; i++) {
            String key = readString(in);
            doc.putRaw(key, readValue(in));
        }
        return doc;
    }

    private static void writeValue(DataOutputStream out, Object v) throws IOException {
        if (v == null) {
            out.writeByte(NULL);
        } else if (v instanceof String) {
            out.writeByte(STRING);
            writeString(out, (String) v);
        } else if (v instanceof Long) {
            out.writeByte(LONG);
            out.writeLong((Long) v);
        } else if (v instanceof Double) {
            out.writeByte(DOUBLE);
            out.writeDouble((Double) v);
        } else if (v instanceof Boolean) {
            out.writeByte(BOOL);
            out.writeBoolean((Boolean) v);
        } else if (v instanceof LocalDate) {
            out.writeByte(DATE);
            out.writeLong(((LocalDate) v).toEpochDay());
        } else if (v instanceof Instant) {
            out.writeByte(INSTANT);
            out.writeLong(((Instant) v).getEpochSecond());
            out.writeInt(((Instant) v).getNano());
        } else if (v instanceof List<?>) {
            List<?> list = (List<?>) v;
            out.writeByte(LIST);
            out.writeInt(list.size());
            for (Object o : list) writeValue(out, o);
        } else if (v instanceof Doc) {
            out.writeByte(DOC);
            writeDoc(out, (Doc) v);
        } else {
            throw new IOException("Unsupported value " + v.getClass());
        }
    }

    private static Object readValue(DataInputStream in) throws IOException {
        byte tag = in.readByte();
        switch (tag) {
            case NULL:
                return null;
            case STRING:
                return readString(in);
            case LONG:
                return in.readLong();
            case DOUBLE:
                return in.readDouble();
            case BOOL:
                return in.readBoolean();
            case DATE:
                return LocalDate.ofEpochDay(in.readLong());
            case INSTANT: {
                long seconds = in.readLong();
                int nanos = in.readInt();
                return Instant.ofEpochSecond(seconds, nanos);
            }
            case LIST: {
                int size = in.readInt();
                if (size < 0 || size > 1_000_000) throw new IOException("Bad list size " + size);
                List<Object> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(readValue(in));
                return list;
            }
            case DOC:
                return readDoc(in);
            default:
                throw new IOException("Unknown value tag " + tag);
        }
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > 16 * 1024 * 1024) throw new IOException("Bad string length " + length);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
