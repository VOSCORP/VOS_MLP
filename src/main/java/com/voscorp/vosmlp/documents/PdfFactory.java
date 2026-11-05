package com.voscorp.vosmlp.documents;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PdfFactory {
    private static final String FROM = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяәғқңөұүһі";
    private static final String[] TO = {"a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "y", "k", "l", "m", "n",
            "o", "p", "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "", "y", "", "e", "yu", "ya",
            "a", "g", "k", "n", "o", "u", "u", "h", "i"};

    private PdfFactory() {}

    // кириллица в латиницу
    public static String translit(String text) {
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            int i = FROM.indexOf(Character.toLowerCase(ch));
            if (i >= 0) {
                String t = TO[i];
                sb.append(Character.isUpperCase(ch) && !t.isEmpty()
                        ? t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1) : t);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String ascii(String text) {
        StringBuilder sb = new StringBuilder();
        for (char ch : translit(text).toCharArray()) {
            if (ch == '(' || ch == ')' || ch == '\\') { sb.append('\\'); }
            sb.append(ch < 128 && ch >= 32 ? ch : '?');
        }
        return sb.toString();
    }

    // Простой PDF на одну страницу: заголовок и строки текста
    public static byte[] simple(String title, List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 20 Tf 50 780 Td (").append(ascii(title)).append(") Tj ET\n");
        int y = 740;
        for (String line : lines) {
            content.append("BT /F1 12 Tf 50 ").append(y).append(" Td (").append(ascii(line)).append(") Tj ET\n");
            y -= 22;
        }
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R "
                + "/Resources << /Font << /F1 5 0 R >> >> >>");
        objects.add("<< /Length " + content.length() + " >>\nstream\n" + content + "endstream");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.size() + 1];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = pdf.length();
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }
}