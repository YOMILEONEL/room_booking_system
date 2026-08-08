package steve.bookingssystem.invoice.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.invoice.model.Invoice;
import steve.bookingssystem.payment.model.Payment;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final float SECTION_SPACING = 16f;

    public byte[] generate(Invoice invoice) {
        Payment payment = invoice.getPayment();
        Booking booking = payment.getBooking();
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.GERMANY);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        Document document = new Document();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(section("Rechnung", titleFont));

            document.add(new Paragraph("Rechnungsnummer: " + invoice.getInvoiceNumber(), textFont));
            document.add(section("Rechnungsdatum: " + invoice.getInvoiceDate().format(DATE_FORMAT), textFont));

            document.add(new Paragraph("Kunde", labelFont));
            document.add(section(invoice.getCustomerUsernameSnapshot(), textFont));

            document.add(new Paragraph("Buchung", labelFont));
            document.add(new Paragraph("Raum: " + invoice.getRoomNameSnapshot(), textFont));
            Paragraph timeframe = new Paragraph(
                    "Zeitraum: " + booking.getStartTime().format(DATE_FORMAT) + " - " + booking.getEndTime().format(DATE_FORMAT),
                    textFont);
            if (payment.getAppliedDiscountCode() == null) {
                timeframe.setSpacingAfter(SECTION_SPACING);
            }
            document.add(timeframe);
            if (payment.getAppliedDiscountCode() != null) {
                document.add(section("Rabattcode: " + payment.getAppliedDiscountCode(), textFont));
            }

            Paragraph amount = new Paragraph("Gesamtbetrag: " + currency.format(payment.getAmount()), labelFont);
            amount.setAlignment(Element.ALIGN_RIGHT);
            document.add(amount);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Rechnung konnte nicht erzeugt werden", e);
        }
    }

    private Paragraph section(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingAfter(SECTION_SPACING);
        return paragraph;
    }
}
