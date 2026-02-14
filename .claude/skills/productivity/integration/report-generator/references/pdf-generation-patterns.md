# PDF Generation Patterns Guide

**Skill:** report-generator
**Component:** iText / Flying Saucer
**Purpose:** Generate PDF documents (invoices, certificates, statements)

---

## Library Comparison

| Library | Use Case | Complexity |
|---------|----------|------------|
| **iText** | Programmatic PDF (tables, forms, signatures) | High |
| **Flying Saucer** | HTML to PDF (CSS styling) | Low |
| **Thymeleaf + Flying Saucer** | Template-based PDF | Medium (Recommended) |

---

## Pattern 1: iText Programmatic PDF

```java
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

@Service
public class InvoicePdfService {

    public String generateInvoice(OrderVO order) throws Exception {
        Document document = new Document(PageSize.A4);
        String filePath = "/tmp/pdfs/invoice_" + order.getOrderId() + ".pdf";
        PdfWriter.getInstance(document, new FileOutputStream(filePath));

        document.open();

        // Add header
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph header = new Paragraph("INVOICE", headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        document.add(new Paragraph("\n"));

        // Add order details
        PdfPTable table = new PdfPTable(4);
        table.addCell("Product");
        table.addCell("Quantity");
        table.addCell("Unit Price");
        table.addCell("Total");

        for (OrderItemVO item : order.getItems()) {
            table.addCell(item.getProductName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell("$" + item.getUnitPrice());
            table.addCell("$" + item.getTotal());
        }

        document.add(table);

        // Add total
        Paragraph total = new Paragraph("Total: $" + order.getTotalAmount());
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.close();
        return filePath;
    }
}
```

---

## Pattern 2: HTML to PDF (Flying Saucer + Thymeleaf)

**Recommended for SmartAdmin**

### Step 1: Create HTML Template

```html
<!-- templates/invoice.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <style>
        body { font-family: Arial, sans-serif; }
        .header { text-align: center; font-size: 24px; font-weight: bold; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; }
        th { background-color: #f2f2f2; }
        .total { text-align: right; font-size: 18px; font-weight: bold; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="header">INVOICE</div>
    <p><strong>Order Number:</strong> <span th:text="${order.orderNumber}"></span></p>
    <p><strong>Date:</strong> <span th:text="${#temporals.format(order.orderDate, 'yyyy-MM-dd')}"></span></p>

    <table>
        <thead>
            <tr>
                <th>Product</th>
                <th>Quantity</th>
                <th>Unit Price</th>
                <th>Total</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="item : ${order.items}">
                <td th:text="${item.productName}"></td>
                <td th:text="${item.quantity}"></td>
                <td th:text="${'$' + item.unitPrice}"></td>
                <td th:text="${'$' + item.total}"></td>
            </tr>
        </tbody>
    </table>

    <div class="total">Total: $<span th:text="${order.totalAmount}"></span></div>
</body>
</html>
```

### Step 2: Generate PDF from Template

```java
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@RequiredArgsConstructor
public class TemplatedPdfService {

    private final TemplateEngine templateEngine;

    public String generateInvoice(OrderVO order) throws Exception {
        // Render HTML from template
        Context context = new Context();
        context.setVariable("order", order);
        String html = templateEngine.process("invoice", context);

        // Convert HTML to PDF
        String filePath = "/tmp/pdfs/invoice_" + order.getOrderId() + ".pdf";
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(fos);
        }

        return filePath;
    }
}
```

---

## Pattern 3: PDF with Images/QR Codes

```java
// Add QR code to PDF
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;

public byte[] generateQRCode(String content) throws Exception {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);

    ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
    return pngOutputStream.toByteArray();
}

// Add to PDF
Image qrImage = Image.getInstance(generateQRCode(order.getOrderNumber()));
document.add(qrImage);
```

---

## SmartAdmin Integration

```java
@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoicePdfController {

    private final TemplatedPdfService pdfService;

    @PostMapping("/generate")
    public void generateInvoice(@RequestBody Long orderId, HttpServletResponse response) {
        try {
            OrderVO order = orderService.getOrder(orderId);
            String filePath = pdfService.generateInvoice(order);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=invoice.pdf");

            try (FileInputStream fis = new FileInputStream(filePath);
                 OutputStream os = response.getOutputStream()) {
                IOUtils.copy(fis, os);
            }
        } catch (Exception e) {
            throw new BusinessException("PDF generation failed");
        }
    }
}
```

---

**Next:** [CSV Streaming](csv-streaming-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
