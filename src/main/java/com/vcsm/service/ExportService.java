package com.vcsm.service;

import com.opencsv.CSVWriter;
import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    @Autowired
    private ComplaintRepository complaintRepository;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Export complaints as CSV
     */
    public ByteArrayInputStream exportComplaintsToCSV() {
        List<Complaint> complaints = complaintRepository.findAll();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {
            
            // Header
            String[] header = {"ID", "Resident", "Category", "Description", "Status", "Priority", "Date"};
            writer.writeNext(header);

            // Data rows
            for (Complaint c : complaints) {
                String[] row = {
                    String.valueOf(c.getId()),
                    c.getResidentName(),
                    c.getCategory() != null ? c.getCategory().toString() : "N/A",
                    c.getDescription() != null ? c.getDescription().substring(0, Math.min(50, c.getDescription().length())) : "",
                    c.getStatus() != null ? c.getStatus().toString() : "N/A",
                    c.getPriority() != null ? c.getPriority() : "MEDIUM",
                    c.getCreatedAt() != null ? dateFormatter.format(c.getCreatedAt()) : ""
                };
                writer.writeNext(row);
            }
        } catch (RuntimeException e) {
            log.error("Failed to export complaints to CSV", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Export complaints as PDF
     */
    public ByteArrayInputStream exportComplaintsToPDF() {
        List<Complaint> complaints = complaintRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Complaints Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Subtitle
            Font subFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
            Paragraph sub = new Paragraph("Generated on: " + java.time.LocalDateTime.now().format(dateFormatter), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            document.add(sub);
            document.add(new Paragraph(" "));

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);

            // Headers
            String[] headers = {"ID", "Resident", "Category", "Status", "Priority", "Date"};
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Data
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 10);
            for (Complaint c : complaints) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(c.getId()), dataFont)));
                table.addCell(new PdfPCell(new Phrase(c.getResidentName(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                    c.getCategory() != null ? c.getCategory().toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                    c.getStatus() != null ? c.getStatus().toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                    c.getPriority() != null ? c.getPriority() : "MEDIUM", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                    c.getCreatedAt() != null ? dateFormatter.format(c.getCreatedAt()) : "", dataFont)));
            }

            document.add(table);
            document.close();

        } catch (RuntimeException e) {
            log.error("Failed to export complaints to PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}