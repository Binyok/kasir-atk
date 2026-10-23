package giriatk.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExcelExporter {

    public enum Kind { TEXT, CENTER, NUM, RP }

    public static final class Cell {
        final String text;
        final Double number;
        final Kind kind;

        private Cell(String text, Double number, Kind kind) {
            this.text = text;
            this.number = number;
            this.kind = kind;
        }
    }

    private enum RowKind { TITLE, SUBTITLE, SECTION, HEADER, DATA, TOTAL, PLAIN, EMPTY }

    private static final class Row {
        final RowKind kind;
        final List<Cell> cells;
        final boolean band;
        final int mergeSpan;
        final int height;

        Row(RowKind kind, List<Cell> cells, boolean band, int mergeSpan, int height) {
            this.kind = kind;
            this.cells = cells;
            this.band = band;
            this.mergeSpan = mergeSpan;
            this.height = height;
        }
    }

    private static final class Sheet {
        final String name;
        final List<Row> rows = new ArrayList<>();
        final List<Integer> widths = new ArrayList<>();
        boolean band;

        Sheet(String name) {
            this.name = name;
        }
    }

    private final List<Sheet> sheets = new ArrayList<>();
    private Sheet current;

    public ExcelExporter() {
    }

    public void sheet(String name) {
        current = new Sheet(name);
        sheets.add(current);
    }

    public static Cell txt(String value) {
        return new Cell(value, null, Kind.TEXT);
    }

    public static Cell ctr(String value) {
        return new Cell(value, null, Kind.CENTER);
    }

    public static Cell num(Number value) {
        return new Cell(null, value == null ? 0 : value.doubleValue(), Kind.NUM);
    }

    public static Cell rp(Number value) {
        return new Cell(null, value == null ? 0 : value.doubleValue(), Kind.RP);
    }

    public void title(String text, int span) {
        add(new Row(RowKind.TITLE, List.of(txt(text)), false, span, 26));
    }

    public void subtitle(String label, String value) {
        add(new Row(RowKind.SUBTITLE, List.of(txt(label), txt(value)), false, 0, 0));
    }

    public void blank() {
        add(new Row(RowKind.PLAIN, List.of(), false, 0, 8));
    }

    public void section(String text, int span) {
        current.band = false;
        add(new Row(RowKind.SECTION, List.of(txt(text)), false, span, 22));
    }

    public void header(String... columns) {
        current.band = false;
        List<Cell> cells = new ArrayList<>();
        for (String c : columns) {
            cells.add(txt(c));
        }
        add(new Row(RowKind.HEADER, cells, false, 0, 22));
    }

    public void row(Cell... cells) {
        add(new Row(RowKind.DATA, List.of(cells), current.band, 0, 18));
        current.band = !current.band;
    }

    public void kosong(String message, int span) {
        List<Cell> cells = new ArrayList<>();
        cells.add(txt(message));
        for (int i = 1; i < Math.max(1, span); i++) {
            cells.add(txt(""));
        }
        add(new Row(RowKind.EMPTY, cells, false, 0, 18));
    }

    public void total(String label, Cell... values) {
        current.band = false;
        List<Cell> cells = new ArrayList<>();
        cells.add(txt(label));
        cells.addAll(List.of(values));
        add(new Row(RowKind.TOTAL, cells, false, 0, 20));
    }

    private void add(Row row) {
        if (current == null) {
            sheet("Laporan");
        }
        current.rows.add(row);
        if (row.kind == RowKind.TITLE || row.kind == RowKind.SECTION) {
            return;
        }
        for (int i = 0; i < row.cells.size(); i++) {
            while (current.widths.size() <= i) {
                current.widths.add(10);
            }
            current.widths.set(i, Math.max(current.widths.get(i), panjang(row.cells.get(i))));
        }
    }

    private int panjang(Cell cell) {
        if (cell.text != null) {
            return cell.text.length();
        }
        if (cell.number == null) {
            return 6;
        }
        long n = Math.abs(cell.number.longValue());
        int digits = String.valueOf(n).length();
        if (digits > 3) {
            digits += (digits - 1) / 3;
        }
        return cell.kind == Kind.RP ? digits + 3 : digits;
    }

    public void save(Path target) throws IOException {
        if (sheets.isEmpty()) {
            sheet("Laporan");
        }
        try (OutputStream os = Files.newOutputStream(target); ZipOutputStream zip = new ZipOutputStream(os)) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", rootRels());
            put(zip, "xl/workbook.xml", workbook());
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            put(zip, "xl/styles.xml", styles());
            for (int i = 0; i < sheets.size(); i++) {
                put(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheetXml(sheets.get(i)));
            }
        }
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private int jumlahKolom(Sheet sheet) {
        int max = 1;
        for (Row r : sheet.rows) {
            max = Math.max(max, r.cells.size());
        }
        return max;
    }

    private String sheetXml(Sheet sheet) {
        int kolom = jumlahKolom(sheet);
        while (sheet.widths.size() < kolom) {
            sheet.widths.add(10);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<sheetViews><sheetView workbookViewId=\"0\"/></sheetViews>");
        sb.append("<sheetFormatPr defaultRowHeight=\"15\"/>");
        sb.append("<cols>");
        for (int i = 0; i < kolom; i++) {
            double w = Math.min(45, Math.max(10, sheet.widths.get(i) + 3));
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(w).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols>");
        sb.append("<sheetData>");

        List<String> merges = new ArrayList<>();
        int r = 1;
        for (Row row : sheet.rows) {
            sb.append("<row r=\"").append(r).append("\"");
            if (row.height > 0) {
                sb.append(" ht=\"").append(row.height).append("\" customHeight=\"1\"");
            }
            sb.append(">");
            for (int c = 0; c < row.cells.size(); c++) {
                Cell cell = row.cells.get(c);
                sb.append("<c r=\"").append(namaKolom(c)).append(r).append("\" s=\"").append(gaya(row, cell, c)).append("\"");
                if (cell.number != null) {
                    sb.append("><v>").append(angka(cell.number)).append("</v></c>");
                } else {
                    sb.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(esc(cell.text)).append("</t></is></c>");
                }
            }
            sb.append("</row>");
            int span = row.mergeSpan;
            if (span > 1) {
                span = Math.min(span, kolom);
                merges.add("A" + r + ":" + namaKolom(span - 1) + r);
            }
            r++;
        }
        sb.append("</sheetData>");
        if (!merges.isEmpty()) {
            sb.append("<mergeCells count=\"").append(merges.size()).append("\">");
            for (String m : merges) {
                sb.append("<mergeCell ref=\"").append(m).append("\"/>");
            }
            sb.append("</mergeCells>");
        }
        sb.append("</worksheet>");
        return sb.toString();
    }

    private int gaya(Row row, Cell cell, int index) {
        switch (row.kind) {
            case TITLE:
                return 1;
            case SUBTITLE:
                return index == 0 ? 2 : 17;
            case SECTION:
                return 3;
            case HEADER:
                return 4;
            case EMPTY:
                return 18;
            case TOTAL:
                if (cell.kind == Kind.RP) {
                    return 14;
                }
                if (cell.kind == Kind.NUM) {
                    return 15;
                }
                return 13;
            case DATA:
                switch (cell.kind) {
                    case NUM:
                        return row.band ? 8 : 7;
                    case RP:
                        return row.band ? 10 : 9;
                    case CENTER:
                        return row.band ? 12 : 11;
                    default:
                        return row.band ? 6 : 5;
                }
            default:
                return 16;
        }
    }

    private String namaKolom(int index) {
        StringBuilder sb = new StringBuilder();
        int i = index;
        while (i >= 0) {
            sb.insert(0, (char) ('A' + (i % 26)));
            i = i / 26 - 1;
        }
        return sb.toString();
    }

    private String angka(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String contentTypes() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 0; i < sheets.size(); i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        sb.append("</Types>");
        return sb.toString();
    }

    private String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbook() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ");
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        sb.append("<sheets>");
        for (int i = 0; i < sheets.size(); i++) {
            sb.append("<sheet name=\"").append(esc(sheets.get(i).name)).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        sb.append("</sheets></workbook>");
        return sb.toString();
    }

    private String workbookRels() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < sheets.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i + 1).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(sheets.size() + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        sb.append("</Relationships>");
        return sb.toString();
    }

    private String styles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"&quot;Rp&quot; #,##0\"/></numFmts>"
                + "<fonts count=\"6\">"
                + "<font><sz val=\"11\"/><color rgb=\"FF1A1D21\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"14\"/><color rgb=\"FF0F172A\"/><name val=\"Calibri\"/></font>"
                + "<font><i/><sz val=\"10\"/><color rgb=\"FF6B7280\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"10\"/><color rgb=\"FFD97706\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FF0F172A\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"6\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD97706\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFEF3C7\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF8FAFC\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFDE68A\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"2\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border>"
                + "<left style=\"thin\"><color rgb=\"FFD5D8DE\"/></left>"
                + "<right style=\"thin\"><color rgb=\"FFD5D8DE\"/></right>"
                + "<top style=\"thin\"><color rgb=\"FFD5D8DE\"/></top>"
                + "<bottom style=\"thin\"><color rgb=\"FFD5D8DE\"/></bottom>"
                + "<diagonal/></border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + cellXfs()
                + "</styleSheet>";
    }

    private String cellXfs() {
        StringBuilder sb = new StringBuilder();
        sb.append("<cellXfs count=\"19\">");
        sb.append(xf(0, 0, 0, 0, null));
        sb.append(xf(0, 1, 0, 0, "left"));
        sb.append(xf(0, 2, 0, 0, "left"));
        sb.append(xf(0, 3, 2, 1, "left"));
        sb.append(xf(0, 4, 3, 1, "center"));
        sb.append(xf(0, 0, 0, 1, "left"));
        sb.append(xf(0, 0, 4, 1, "left"));
        sb.append(xf(1, 0, 0, 1, "right"));
        sb.append(xf(1, 0, 4, 1, "right"));
        sb.append(xf(164, 0, 0, 1, "right"));
        sb.append(xf(164, 0, 4, 1, "right"));
        sb.append(xf(0, 0, 0, 1, "center"));
        sb.append(xf(0, 0, 4, 1, "center"));
        sb.append(xf(0, 5, 5, 1, "left"));
        sb.append(xf(164, 5, 5, 1, "right"));
        sb.append(xf(1, 5, 5, 1, "right"));
        sb.append(xf(0, 0, 0, 0, "left"));
        sb.append(xf(0, 5, 0, 0, "left"));
        sb.append(xf(0, 2, 0, 1, "center"));
        sb.append("</cellXfs>");
        return sb.toString();
    }

    private String xf(int numFmtId, int fontId, int fillId, int borderId, String align) {
        StringBuilder sb = new StringBuilder();
        sb.append("<xf numFmtId=\"").append(numFmtId).append("\" fontId=\"").append(fontId)
                .append("\" fillId=\"").append(fillId).append("\" borderId=\"").append(borderId)
                .append("\" xfId=\"0\"");
        if (numFmtId != 0) {
            sb.append(" applyNumberFormat=\"1\"");
        }
        if (fontId != 0) {
            sb.append(" applyFont=\"1\"");
        }
        if (fillId != 0) {
            sb.append(" applyFill=\"1\"");
        }
        if (borderId != 0) {
            sb.append(" applyBorder=\"1\"");
        }
        if (align != null) {
            sb.append(" applyAlignment=\"1\"><alignment horizontal=\"").append(align)
                    .append("\" vertical=\"center\"/></xf>");
        } else {
            sb.append("/>");
        }
        return sb.toString();
    }
}
