package giriatk.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public final class Icons {

    private Icons() {
    }

    public enum Name {
        DASHBOARD, CART, QUEUE, BOX, TAG, PRINTER, CHART, USERS, SETTINGS, LOGOUT,
        PLUS, MINUS, EDIT, TRASH, SEARCH, REFRESH, DOWNLOAD, WARNING, CHECK, CLOSE,
        RECEIPT, MONEY, CLOCK, CALENDAR, EYE, KEY
    }

    public static void paint(Graphics2D target, Name name, int size, Color color) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (name) {
            case DASHBOARD:
                g.draw(new RoundRectangle2D.Double(3, 3, 7.5, 7.5, 2.4, 2.4));
                g.draw(new RoundRectangle2D.Double(13.5, 3, 7.5, 4.5, 2.2, 2.2));
                g.draw(new RoundRectangle2D.Double(13.5, 10.5, 7.5, 10.5, 2.4, 2.4));
                g.draw(new RoundRectangle2D.Double(3, 13.5, 7.5, 7.5, 2.4, 2.4));
                break;
            case CART:
                g.draw(new Line2D.Double(3, 4.5, 5.4, 4.5));
                g.draw(new Line2D.Double(5.4, 4.5, 7.2, 15.5));
                g.draw(new Line2D.Double(7.2, 15.5, 19, 15.5));
                g.draw(new Line2D.Double(7.6, 7.5, 20.5, 7.5));
                g.draw(new Line2D.Double(20.5, 7.5, 19, 15.5));
                g.draw(new Ellipse2D.Double(8.5, 18, 2.4, 2.4));
                g.draw(new Ellipse2D.Double(17, 18, 2.4, 2.4));
                break;
            case QUEUE:
                g.draw(new Ellipse2D.Double(4, 3.5, 5, 5));
                g.draw(new Line2D.Double(3, 14.5, 10, 14.5));
                g.draw(new RoundRectangle2D.Double(11, 9.5, 10, 4, 1.6, 1.6));
                g.draw(new RoundRectangle2D.Double(11, 16, 10, 4, 1.6, 1.6));
                break;
            case BOX:
                Path2D box = new Path2D.Double();
                box.moveTo(12, 3);
                box.lineTo(20.5, 7.5);
                box.lineTo(20.5, 16.5);
                box.lineTo(12, 21);
                box.lineTo(3.5, 16.5);
                box.lineTo(3.5, 7.5);
                box.closePath();
                g.draw(box);
                g.draw(new Line2D.Double(3.5, 7.5, 12, 12));
                g.draw(new Line2D.Double(20.5, 7.5, 12, 12));
                g.draw(new Line2D.Double(12, 12, 12, 21));
                break;
            case TAG:
                Path2D tag = new Path2D.Double();
                tag.moveTo(3.5, 10.5);
                tag.lineTo(10.5, 3.5);
                tag.lineTo(20.5, 3.5);
                tag.lineTo(20.5, 13.5);
                tag.lineTo(13.5, 20.5);
                tag.closePath();
                g.draw(tag);
                g.draw(new Ellipse2D.Double(14.6, 6.6, 3.0, 3.0));
                break;
            case PRINTER:
                g.draw(new RoundRectangle2D.Double(6.5, 3, 11, 5.5, 1.6, 1.6));
                g.draw(new RoundRectangle2D.Double(3.5, 8.5, 17, 8, 2.4, 2.4));
                g.draw(new RoundRectangle2D.Double(6.5, 14.5, 11, 6.5, 1.6, 1.6));
                g.draw(new Line2D.Double(17.5, 11.5, 17.6, 11.5));
                break;
            case CHART:
                g.draw(new Line2D.Double(4, 4, 4, 20));
                g.draw(new Line2D.Double(4, 20, 20, 20));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(8.5, 15.5, 8.5, 11.5));
                g.draw(new Line2D.Double(12.5, 15.5, 12.5, 7.5));
                g.draw(new Line2D.Double(16.5, 15.5, 16.5, 10.5));
                break;
            case USERS:
                g.draw(new Ellipse2D.Double(4.5, 4.2, 6.6, 6.6));
                g.draw(new Arc2D.Double(1.5, 12.5, 12.5, 12.5, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(13.5, 5, 6, 6, 300, 220, Arc2D.OPEN));
                g.draw(new Arc2D.Double(14, 13, 9, 9, 20, 140, Arc2D.OPEN));
                break;
            case SETTINGS:
                g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    double x1 = 12 + Math.cos(a) * 6.4;
                    double y1 = 12 + Math.sin(a) * 6.4;
                    double x2 = 12 + Math.cos(a) * 9;
                    double y2 = 12 + Math.sin(a) * 9;
                    g.draw(new Line2D.Double(x1, y1, x2, y2));
                }
                break;
            case LOGOUT:
                g.draw(new Arc2D.Double(3.5, 3.5, 13, 17, 90, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(12, 12, 21, 12));
                g.draw(new Line2D.Double(17.6, 8.6, 21, 12));
                g.draw(new Line2D.Double(17.6, 15.4, 21, 12));
                break;
            case PLUS:
                g.draw(new Line2D.Double(12, 5, 12, 19));
                g.draw(new Line2D.Double(5, 12, 19, 12));
                break;
            case MINUS:
                g.draw(new Line2D.Double(5, 12, 19, 12));
                break;
            case EDIT:
                g.draw(new Line2D.Double(4, 20, 8, 19.2));
                g.draw(new Line2D.Double(4, 20, 4.8, 16));
                g.draw(new Path2D.Double() {{ moveTo(4.8, 16); lineTo(15.5, 5.3); lineTo(18.7, 8.5); lineTo(8, 19.2); }});
                g.draw(new Line2D.Double(13.5, 7.3, 16.7, 10.5));
                break;
            case TRASH:
                g.draw(new Line2D.Double(4, 6.5, 20, 6.5));
                g.draw(new Line2D.Double(9, 6.5, 9.5, 3.5));
                g.draw(new Line2D.Double(15, 6.5, 14.5, 3.5));
                g.draw(new Line2D.Double(9.5, 3.5, 14.5, 3.5));
                g.draw(new Path2D.Double() {{ moveTo(6, 6.5); lineTo(7, 20.5); lineTo(17, 20.5); lineTo(18, 6.5); }});
                g.draw(new Line2D.Double(10, 10.5, 10.3, 16.5));
                g.draw(new Line2D.Double(14, 10.5, 13.7, 16.5));
                break;
            case SEARCH:
                g.draw(new Ellipse2D.Double(4.5, 4.5, 11, 11));
                g.draw(new Line2D.Double(14.5, 14.5, 20, 20));
                break;
            case REFRESH:
                g.draw(new Arc2D.Double(4, 4, 16, 16, 40, 260, Arc2D.OPEN));
                g.draw(new Line2D.Double(19.5, 3.5, 20, 8.5));
                g.draw(new Line2D.Double(19.5, 3.5, 14.6, 4.8));
                break;
            case DOWNLOAD:
                g.draw(new Line2D.Double(12, 3.5, 12, 15));
                g.draw(new Line2D.Double(7.5, 10.5, 12, 15));
                g.draw(new Line2D.Double(16.5, 10.5, 12, 15));
                g.draw(new Path2D.Double() {{ moveTo(4, 17); lineTo(4, 20.5); lineTo(20, 20.5); lineTo(20, 17); }});
                break;
            case WARNING:
                g.draw(new Path2D.Double() {{ moveTo(12, 3.5); lineTo(21.5, 20); lineTo(2.5, 20); closePath(); }});
                g.draw(new Line2D.Double(12, 9.5, 12, 14.5));
                g.draw(new Line2D.Double(12, 17, 12.01, 17));
                break;
            case CHECK:
                g.draw(new Path2D.Double() {{ moveTo(4.5, 12.5); lineTo(9.5, 17.5); lineTo(19.5, 6.5); }});
                break;
            case CLOSE:
                g.draw(new Line2D.Double(6, 6, 18, 18));
                g.draw(new Line2D.Double(18, 6, 6, 18));
                break;
            case RECEIPT:
                g.draw(new RoundRectangle2D.Double(6, 3, 12, 18, 2.4, 2.4));
                g.draw(new Line2D.Double(9, 8, 15, 8));
                g.draw(new Line2D.Double(9, 11.5, 15, 11.5));
                g.draw(new Line2D.Double(9, 15, 13, 15));
                break;
            case MONEY:
                g.draw(new RoundRectangle2D.Double(3, 6.5, 18, 11, 2.2, 2.2));
                g.draw(new Ellipse2D.Double(9.5, 9.5, 5, 5));
                g.draw(new Line2D.Double(6.5, 10, 6.5, 14));
                g.draw(new Line2D.Double(17.5, 10, 17.5, 14));
                break;
            case CLOCK:
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                g.draw(new Line2D.Double(12, 7.5, 12, 12));
                g.draw(new Line2D.Double(12, 12, 15.5, 14));
                break;
            case EYE:
                Path2D eye = new Path2D.Double();
                eye.moveTo(2.5, 12);
                eye.curveTo(7, 5.8, 17, 5.8, 21.5, 12);
                eye.curveTo(17, 18.2, 7, 18.2, 2.5, 12);
                eye.closePath();
                g.draw(eye);
                g.draw(new Ellipse2D.Double(9.4, 9.4, 5.2, 5.2));
                break;
            case KEY:
                g.draw(new Ellipse2D.Double(3.5, 8.5, 7, 7));
                g.draw(new Line2D.Double(10.5, 12, 21, 12));
                g.draw(new Line2D.Double(18, 12, 18, 15.6));
                g.draw(new Line2D.Double(15, 12, 15, 15.2));
                break;
            case CALENDAR:
                g.draw(new RoundRectangle2D.Double(3.5, 5, 17, 15.5, 2.4, 2.4));
                g.draw(new Line2D.Double(3.5, 9.5, 20.5, 9.5));
                g.draw(new Line2D.Double(8, 3, 8, 6.5));
                g.draw(new Line2D.Double(16, 3, 16, 6.5));
                g.draw(new Rectangle2D.Double(7, 13, 3, 3));
                break;
            default:
                break;
        }
        g.dispose();
    }

    public static void paintLogo(Graphics2D target, int size, Color tile, Color glyph) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double r = size * 0.3;
        g.setColor(tile);
        g.fill(new RoundRectangle2D.Double(0, 0, size, size, r, r));

        g.setColor(glyph);
        g.setStroke(new BasicStroke(size * 0.075f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double u = size / 24.0;
        Path2D sheet = new Path2D.Double();
        sheet.moveTo(7 * u, 5 * u);
        sheet.lineTo(14 * u, 5 * u);
        sheet.lineTo(18 * u, 9 * u);
        sheet.lineTo(18 * u, 19 * u);
        sheet.lineTo(7 * u, 19 * u);
        sheet.closePath();
        g.draw(sheet);
        g.draw(new Line2D.Double(14 * u, 5 * u, 14 * u, 9 * u));
        g.draw(new Line2D.Double(14 * u, 9 * u, 18 * u, 9 * u));
        g.dispose();
    }
}
