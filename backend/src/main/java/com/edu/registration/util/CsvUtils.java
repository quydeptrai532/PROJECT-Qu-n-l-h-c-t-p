package com.edu.registration.util;
import java.util.List;

public final class CsvUtils {
    /** BOM UTF-8 để Excel mở tiếng Việt đúng. */
    public static final String BOM = "\uFEFF";
    public static final String XUONG_DONG = "\r\n";

    /** Ký tự đầu khiến Excel/Sheets hiểu ô là công thức (gồm cả dạng toàn góc). */
    private static final String KY_TU_CONG_THUC = "=+-@\uFF1D\uFF0B\uFF0D\uFF20";

    private CsvUtils() {
    }

    /** Chuẩn hóa một ô: trả về chuỗi đã có dấu ngoặc kép bao quanh. */
    public static String cell(String value) {
        String text = value == null ? "" : value;
        StringBuilder sach = new StringBuilder(text.length());
        boolean truocLaDieuKhien = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isISOControl(ch)) {
                // Gộp chuỗi ký tự điều khiển liên tiếp (vd. CRLF) thành đúng một dấu cách.
                if (!truocLaDieuKhien) {
                    sach.append(' ');
                }
                truocLaDieuKhien = true;
            } else {
                sach.append(ch);
                truocLaDieuKhien = false;
            }
        }
        String noiDung = sach.toString().stripLeading();
        if (!noiDung.isEmpty() && KY_TU_CONG_THUC.indexOf(noiDung.charAt(0)) >= 0) {
            noiDung = "'" + noiDung;
        }
        return "\"" + noiDung.replace("\"", "\"\"") + "\"";
    }

    /** Ghép một dòng CSV (kết thúc bằng CRLF). */
    public static String row(List<String> cells) {
        StringBuilder dong = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                dong.append(',');
            }
            dong.append(cell(cells.get(i)));
        }
        return dong.append(XUONG_DONG).toString();
    }
}
