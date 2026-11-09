package giriatk.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PdfExporter {

    private final List<byte[]> streams = new ArrayList<>();
    private final List<Integer> offsets = new ArrayList<>();
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private final List<String> objects = new ArrayList<>();

    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 842f;
    private static final float MARGIN = 45f;
    private static final float LINE_H = 14f;
    private static final float CONTENT_W = PAGE_W - MARGIN * 2;

    private final List<Row> rows = new ArrayList<>();
    private final List<String> colHeaders = new ArrayList<>();
    private final List<Float> colWidths = new ArrayList<>();

    private String docTitle = "";
    private String docSubtitle = "";
    private String docPeriode = "";

    private enum RowKind { SECTION, DATA }

    private static final class Row {
        final RowKind kind;
        final String[] cells;
        Row(RowKind kind, String... cells) {
            this.kind = kind;
            this.cells = cells;
        }
    }

    public PdfExporter setTitle(String title) {
        this.docTitle = title == null ? "" : title;
        return this;
    }

    public PdfExporter setSubtitle(String subtitle) {
        this.docSubtitle = subtitle == null ? "" : subtitle;
        return this;
    }

    public PdfExporter setPeriode(String periode) {
        this.docPeriode = periode == null ? "" : periode;
        return this;
    }

    public PdfExporter setColumns(String[] headers, float[] widths) {
        colHeaders.clear();
        colWidths.clear();
        for (String h : headers) colHeaders.add(h);
        for (float w : widths) colWidths.add(w);
        return this;
    }

    public PdfExporter section(String label) {
        rows.add(new Row(RowKind.SECTION, label));
        return this;
    }

    public PdfExporter row(String... cells) {
        rows.add(new Row(RowKind.DATA, cells));
        return this;
    }

    public void save(Path target) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");

        List<byte[]> objectBytes = new ArrayList<>();

        byte[] page = buildPageStream();
        objectBytes.add(fontDictBytes());
        objectBytes.add(pagesDictBytes());
        objectBytes.add(pageDictBytes(page.length));
        objectBytes.add(page);

        List<Integer> xrefOffsets = new ArrayList<>();
        byte[] header = sb.toString().getBytes(StandardCharsets.ISO_8859_1);
        int pos = header.length;

        List<byte[]> allObjects = new ArrayList<>();
        for (int i = 0; i < objectBytes.size(); i++) {
            String prefix = (i + 1) + " 0 obj\n";
            String suffix = "\nendobj\n";
            byte[] p = prefix.getBytes(StandardCharsets.ISO_8859_1);
            byte[] s = suffix.getBytes(StandardCharsets.ISO_8859_1);
            xrefOffsets.add(pos);
            pos += p.length + objectBytes.get(i).length + s.length;
            allObjects.add(p);
            allObjects.add(objectBytes.get(i));
            allObjects.add(s);
        }

        StringBuilder xref = new StringBuilder("xref\n0 ");
        xref.append(objectBytes.size() + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (int off : xrefOffsets) {
            xref.append(String.format("%010d 00000 n \n", off));
        }
        xref.append("trailer\n<< /Size ").append(objectBytes.size() + 1)
                .append(" /Root 2 0 R >>\nstartxref\n").append(pos).append("\n%%EOF");

        try (OutputStream os = Files.newOutputStream(target)) {
            os.write(header);
            for (byte[] chunk : allObjects) {
                os.write(chunk);
            }
            os.write(xref.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private byte[] fontDictBytes() {
        return "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"
                .getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] pagesDictBytes() {
        return "<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] pageDictBytes(int streamLen) {
        String d = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 1 0 R /F2 1 0 R >> >> "
                + "/Contents 4 0 R >>";
        return d.getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] buildPageStream() throws IOException {
        StringBuilder s = new StringBuilder();
        float y = PAGE_H - MARGIN - 20f;

        s.append("BT\n");
        s.append("/F1 15 Tf\n");
        s.append(MARGIN + " " + y + " Td\n");
        s.append("(" + esc(docTitle) + ") Tj\n");
        y -= 20f;

        if (!docSubtitle.isEmpty()) {
            s.append("ET\nBT\n/F1 9 Tf\n");
            s.append(MARGIN + " " + y + " Td\n");
            s.append("(" + esc(docSubtitle) + ") Tj\n");
            y -= 13f;
        }

        if (!docPeriode.isEmpty()) {
            s.append("ET\nBT\n/F1 9 Tf\n");
            s.append(MARGIN + " " + y + " Td\n");
            s.append("(Periode: " + esc(docPeriode) + ") Tj\n");
            y -= 13f;
        }

        y -= 10f;
        s.append("ET\n");

        s.append(hline(y));
        y -= 5f;

        if (!colHeaders.isEmpty()) {
            s.append("BT\n/F1 8 Tf\n");
            float x = MARGIN;
            for (int i = 0; i < colHeaders.size(); i++) {
                float w = colWidth(i);
                s.append(x + " " + y + " Td\n").append("(" + esc(colHeaders.get(i)) + ") Tj\n").append("ET\nBT\n/F1 8 Tf\n");
                x += w;
            }
            s.append("ET\n");
            y -= LINE_H - 1f;
            s.append(hline(y));
            y -= 4f;
        }

        boolean odd = false;
        for (Row row : rows) {
            if (y < MARGIN + 20f) {
                break;
            }
            if (row.kind == RowKind.SECTION) {
                y -= 6f;
                s.append("BT\n/F1 9 Tf\n");
                s.append(MARGIN + " " + y + " Td\n");
                s.append("(" + esc(row.cells[0]) + ") Tj\n");
                s.append("ET\n");
                y -= LINE_H;
                odd = false;
            } else {
                if (odd) {
                    s.append(shadedRow(y));
                }
                s.append("BT\n/F1 8 Tf\n");
                float x = MARGIN;
                for (int i = 0; i < row.cells.length; i++) {
                    float w = colWidth(i);
                    String cell = i < row.cells.length ? row.cells[i] : "";
                    s.append(x + " " + y + " Td\n");
                    s.append("(" + esc(cell) + ") Tj\n");
                    s.append("ET\nBT\n/F1 8 Tf\n");
                    x += w;
                }
                s.append("ET\n");
                y -= LINE_H;
                odd = !odd;
            }
        }

        s.append(hline(y - 2f));

        s.append("BT\n/F1 7 Tf\n");
        s.append(MARGIN + " " + (MARGIN - 10f) + " Td\n");
        s.append("(Dicetak oleh sistem GiriATK) Tj\n");
        s.append("ET\n");

        byte[] raw = s.toString().getBytes(StandardCharsets.ISO_8859_1);
        String wrap = "<< /Length " + raw.length + " >>\nstream\n"
                + new String(raw, StandardCharsets.ISO_8859_1)
                + "endstream";
        return wrap.getBytes(StandardCharsets.ISO_8859_1);
    }

    private float colWidth(int i) {
        if (colWidths.isEmpty()) return CONTENT_W;
        return i < colWidths.size() ? colWidths.get(i) * CONTENT_W : 50f;
    }

    private String hline(float y) {
        return String.format("%.2f %.2f m %.2f %.2f l S\n", MARGIN, y, PAGE_W - MARGIN, y);
    }

    private String shadedRow(float y) {
        return String.format("0.95 0.95 0.95 rg %.2f %.2f %.2f %.2f re f 0 0 0 rg\n",
                MARGIN, y - 2f, CONTENT_W, LINE_H);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace("\n", " ").replace("\r", " ");
    }
}
