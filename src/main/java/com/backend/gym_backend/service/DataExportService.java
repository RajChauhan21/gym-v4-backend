package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.*;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class DataExportService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    public byte[] exportMembers(Long ownerId,
                                String dueAmount,
                                Integer source,
                                LocalDate joinedFrom,
                                LocalDate joinedTo,
                                LocalDate expiryFrom,
                                LocalDate expiryTo,
                                LocalDate startDateFrom,
                                LocalDate startDateTo,
                                String plan,
                                Integer isActive) {
        Optional<Subscription> subscription = subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(Math.toIntExact(ownerId), List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE));

        if (subscription.isPresent()){
            if(!subscription.get().getName().equals("Max Pro")){
                return new byte[]{};
            }
        }
        else {
            throw new RuntimeException("100");
        }

        Page<MemberProjection> membersData =
                memberRepository.findAllMembersByOwnerId(
                        ownerId,
                        "",
                        dueAmount,
                        source,
                        joinedFrom,
                        joinedTo,
                        expiryFrom,
                        expiryTo,
                        startDateFrom,
                        startDateTo,
                        plan,
                        isActive,
                        null
                );

        List<MemberExportDto> members =
                membersData.stream()
                        .map(this::mapToMemberExportDto)
                        .toList();

        long totalMembers = members.size();

        long activeMembers = members.stream()
                .filter(m -> "Active".equalsIgnoreCase(m.getStatus()))
                .count();

        long inactiveMembers = totalMembers - activeMembers;

        String[] headers = {
                "Name",
                "Phone",
                "Email",
                "Plan",
                "Source",
                "Joining Date",
                "Start Date",
                "Expiry Date",
                "Due Amount",
                "Status"
        };

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("Members");

            // =========================
            // TITLE STYLE
            // =========================

            CellStyle titleStyle = workbook.createCellStyle();

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // =========================
            // HEADER STYLE
            // =========================

            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);

            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );

            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // =========================
            // DATA STYLE
            // =========================

            CellStyle dataStyle = workbook.createCellStyle();

            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // =========================
            // DATE STYLE
            // =========================

            CreationHelper creationHelper =
                    workbook.getCreationHelper();

            CellStyle dateStyle =
                    workbook.createCellStyle();

            dateStyle.cloneStyleFrom(dataStyle);

            dateStyle.setDataFormat(
                    creationHelper
                            .createDataFormat()
                            .getFormat("dd-MMM-yyyy")
            );

            // =========================
            // TITLE ROW
            // =========================

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(25);

            Cell titleCell = titleRow.createCell(0); //9359116709

            titleCell.setCellValue(
                    "Gym Members Report"
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                    new CellRangeAddress(
                            0,
                            0,
                            0,
                            headers.length - 1
                    )
            );

            // =========================
            // SUMMARY ROWS
            // =========================

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy hh:mm a"
                    );

            Row generatedRow = sheet.createRow(1);

            generatedRow.createCell(0)
                    .setCellValue("Generated On");

            generatedRow.createCell(1)
                    .setCellValue(
                            LocalDateTime.now()
                                    .format(formatter)
                    );

            Row totalRow = sheet.createRow(2);

            totalRow.createCell(0)
                    .setCellValue("Total Members");

            totalRow.createCell(1)
                    .setCellValue(totalMembers);

            Row activeRow = sheet.createRow(3);

            activeRow.createCell(0)
                    .setCellValue("Active Members");

            activeRow.createCell(1)
                    .setCellValue(activeMembers);

            Row inactiveRow = sheet.createRow(4);

            inactiveRow.createCell(0)
                    .setCellValue("Inactive Members");

            inactiveRow.createCell(1)
                    .setCellValue(inactiveMembers);

            // Row 5 left blank intentionally

            // =========================
            // HEADER ROW
            // =========================

            Row headerRow = sheet.createRow(6);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }

            // =========================
            // DATA ROWS
            // =========================

            int rowNum = 7;

            for (MemberExportDto member : members) {

                Row row = sheet.createRow(rowNum++);

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(
                        member.getName() != null
                                ? member.getName()
                                : ""
                );
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(
                        member.getPhone() != null
                                ? member.getPhone()
                                : ""
                );
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(
                        member.getEmail() != null
                                ? member.getEmail()
                                : ""
                );
                cell2.setCellStyle(dataStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(
                        member.getMembershipName() != null
                                ? member.getMembershipName()
                                : ""
                );
                cell3.setCellStyle(dataStyle);

                Cell cell4 = row.createCell(4);

                if (member.getJoinedDate() != null) {

                    cell4.setCellValue(
                            !member.getSource().trim().isBlank()?member.getSource():""
                    );

                    cell4.setCellStyle(dateStyle);
                }

                Cell cell5 = row.createCell(5);

                if (member.getJoinedDate() != null) {

                    cell5.setCellValue(
                            java.sql.Date.valueOf(
                                    member.getJoinedDate()
                            )
                    );

                    cell5.setCellStyle(dateStyle);
                }

                Cell cell6 = row.createCell(6);
                
                cell6.setCellStyle(dataStyle);

                if (member.getStartDate() != null) {

                    cell6.setCellValue(
                            member.getStartDate()
                                    .format(
                                            DateTimeFormatter.ofPattern(
                                                    "yyyy-MM-dd"
                                            )
                                    )
                    );

                }

                Cell cell7 = row.createCell(7);

                if (member.getExpiryDate() != null) {

                    cell7.setCellValue(
                            java.sql.Date.valueOf(
                                    member.getExpiryDate()
                            )
                    );

                    cell7.setCellStyle(dateStyle);
                }

                Cell cell8 = row.createCell(8);

                cell8.setCellValue(
                        member.getDueAmount()
                );

                cell8.setCellStyle(dataStyle);

                Cell cell9 = row.createCell(9);

                cell9.setCellValue(
                        member.getStatus()
                );

                cell9.setCellStyle(dataStyle);
            }

            // =========================
            // FILTERS
            // =========================

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            6,
                            6,
                            0,
                            headers.length - 1
                    )
            );

            // =========================
            // FREEZE HEADER
            // =========================

            sheet.createFreezePane(
                    0,
                    7
            );

            // =========================
            // COLUMN WIDTHS
            // =========================

            sheet.setColumnWidth(0, 6000);   // Name
            sheet.setColumnWidth(1, 5000);   // Phone
            sheet.setColumnWidth(2, 6000);  // Email
            sheet.setColumnWidth(3, 6000);   // Plan
            sheet.setColumnWidth(4, 5000);   // source
            sheet.setColumnWidth(5, 4500);   // Joining Date
            sheet.setColumnWidth(6, 4500);   // Start Date
            sheet.setColumnWidth(7, 4500);   // Expiry Date
            sheet.setColumnWidth(8, 4500);   // Due Amount
            sheet.setColumnWidth(9, 4000);   // Status

            workbook.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to export members report",
                    e
            );
        }
    }

    public byte[] exportPayments(Long ownerId,
                                 String dueAmount,
                                 String paidAmount,
                                 LocalDate dateFrom,
                                 LocalDate dateTo,
                                 String method,
                                 String plan) {

        Optional<Subscription> subscription = subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(Math.toIntExact(ownerId), List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE));

        if (subscription.isPresent()){
            if(!subscription.get().getName().equals("Max Pro")){
                return new byte[]{};
            }
        }
        else {
            throw new RuntimeException("100");
        }


        Page<PaymentProjection> paymentsData = paymentRepository.findPaymentsFiltered(ownerId, "", plan, method, paidAmount, dueAmount, dateFrom, dateTo, null);

        List<PaymentExportDto> payments =
                paymentsData.stream()
                        .map(this::mapToPaymentExportDto)
                        .toList();

        long totalTransactions = payments.size();

        double totalPaid = payments.stream()
                .mapToDouble(p ->
                        p.getPaidAmount() != null
                                ? p.getPaidAmount()
                                : 0)
                .sum();

        double totalDue = payments.stream()
                .mapToDouble(p ->
                        p.getDueAmount() != null
                                ? p.getDueAmount()
                                : 0)
                .sum();

        String[] headers = {
                "Name",
                "Plan",
                "Amount Paid",
                "Amount Due",
                "Date",
                "Method"
        };

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("Payments");

            // =========================
            // TITLE STYLE
            // =========================

            CellStyle titleStyle = workbook.createCellStyle();

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // ========================
            // Summary Style
            //=========================
            CellStyle summaryStyle = workbook.createCellStyle();

            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);

            summaryStyle.setFont(summaryFont);

            // =========================
            // HEADER STYLE
            // =========================

            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);

            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );

            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // =========================
            // DATA STYLE
            // =========================

            CellStyle dataStyle = workbook.createCellStyle();

            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // =========================
            // DATE STYLE
            // =========================

            //=========================
            // Currency Style
            //==========================
            CellStyle currencyStyle = workbook.createCellStyle();

            currencyStyle.cloneStyleFrom(dataStyle);

            currencyStyle.setDataFormat(
                    workbook.createDataFormat()
                            .getFormat("₹#,##0.00")
            );

            CreationHelper creationHelper =
                    workbook.getCreationHelper();

            CellStyle dateStyle =
                    workbook.createCellStyle();

            dateStyle.cloneStyleFrom(dataStyle);

            dateStyle.setDataFormat(
                    creationHelper
                            .createDataFormat()
                            .getFormat("dd-MMM-yyyy")
            );

            // =========================
            // TITLE ROW
            // =========================

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(25);

            Cell titleCell = titleRow.createCell(0);

            titleCell.setCellValue(
                    "Gym Payments Report"
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                    new CellRangeAddress(
                            0,
                            0,
                            0,
                            headers.length - 1
                    )
            );

            // =========================
            // SUMMARY ROWS
            // =========================

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy hh:mm a"
                    );

            Row generatedRow = sheet.createRow(1);

            generatedRow.createCell(0)
                    .setCellValue("Generated On");

            generatedRow.createCell(1)
                    .setCellValue(
                            LocalDateTime.now()
                                    .format(formatter)
                    );

            generatedRow.getCell(0)
                    .setCellStyle(summaryStyle);

            Row totalTxnRow = sheet.createRow(2);

            totalTxnRow.createCell(0)
                    .setCellValue("Total Transactions");

            totalTxnRow.createCell(1)
                    .setCellValue(totalTransactions);

            totalTxnRow.getCell(0)
                    .setCellStyle(summaryStyle);

            Row totalPaidRow = sheet.createRow(3);

            totalPaidRow.createCell(0)
                    .setCellValue("Total Amount Paid");

            totalPaidRow.createCell(1)
                    .setCellValue(totalPaid);

            totalPaidRow.getCell(0)
                    .setCellStyle(summaryStyle);

            Row totalDueRow = sheet.createRow(4);

            totalDueRow.createCell(0)
                    .setCellValue("Total Amount Due");

            totalDueRow.createCell(1)
                    .setCellValue(totalDue);

            totalDueRow.getCell(0)
                    .setCellStyle(summaryStyle);

            // Row 5 left blank intentionally

            // =========================
            // HEADER ROW
            // =========================

            Row headerRow = sheet.createRow(6);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }

            // =========================
            // DATA ROWS
            // =========================

            int rowNum = 7;

            for (PaymentExportDto payment : payments) {

                Row row = sheet.createRow(rowNum++);

                //member name
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(
                        payment.getName() != null
                                ? payment.getName()
                                : ""
                );
                cell0.setCellStyle(dataStyle);

                //plan name
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(
                        payment.getPlan() != null
                                ? payment.getPlan()
                                : ""
                );
                cell1.setCellStyle(dataStyle);

                //Amount paid
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(
                        payment.getPaidAmount() != null
                                ? payment.getPaidAmount()
                                : 0
                );
                cell2.setCellStyle(currencyStyle);

                //Amount due

                Cell cell3 = row.createCell(3);

                cell3.setCellValue(
                        payment.getDueAmount()
                );
                cell3.setCellStyle(currencyStyle);

                //Date
                Cell cell4 = row.createCell(4);

                cell4.setCellValue(
                        java.sql.Date.valueOf(
                                payment.getDate()
                        )
                );

                cell4.setCellStyle(dateStyle);

                //method
                Cell cell5 = row.createCell(5);

                cell5.setCellValue(
                        payment.getMethod()
                );
                cell5.setCellStyle(dataStyle);
            }

            // =========================
            // FILTERS
            // =========================

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            6,
                            6,
                            0,
                            headers.length - 1
                    )
            );

            // =========================
            // FREEZE HEADER
            // =========================

            sheet.createFreezePane(
                    0,
                    7
            );

            // =========================
            // COLUMN WIDTHS
            // =========================

            sheet.setColumnWidth(0, 6000);   // Name
            sheet.setColumnWidth(1, 6000);   // Plan
            sheet.setColumnWidth(2, 4500);   // Amount Paid
            sheet.setColumnWidth(3, 4500);   // Amount Due
            sheet.setColumnWidth(4, 4500);   //  Date
            sheet.setColumnWidth(5, 4500);   // Method

            sheet.getRow(6).setHeightInPoints(22);

            workbook.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to export Payments report",
                    e
            );
        }
    }

    public byte[] exportSubscriptions(Long ownerId,
                                      String amount,
                                      String method,
                                      String status,
                                      LocalDateTime fromDate,
                                      LocalDateTime toDate) {

        Optional<Subscription> ss = subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(Math.toIntExact(ownerId), List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE));

        if (ss.isPresent()){
            if(!ss.get().getName().equals("Max Pro")){
                return new byte[]{};
            }
        }
        else {
            throw new RuntimeException("100");
        }

        com.backend.gym_backend.enums.Payment payment = null;
        if (status != null && status.equals("SUCCESS")) {
            payment = com.backend.gym_backend.enums.Payment.CAPTURED;
        }
        if (status != null && status.equals("FAILED")) {
            payment = com.backend.gym_backend.enums.Payment.FAILED;
        }

        Page<OwnerPaymentProjection> subscriptionsRecord = ownerPaymentRepository.findPaymentsByOwner(Math.toIntExact(ownerId), amount, payment, method, fromDate, toDate, null);

        List<SubscriptionExportDto> subscription =
                subscriptionsRecord.stream()
                        .map(this::mapToSubscriptionExportDto)
                        .toList();

        long totalTransactions = subscription.size();

        long successCount = subscription.stream()
                .filter(s -> "CAPTURED".equalsIgnoreCase(s.getStatus()))
                .count();

        long failedCount = subscription.stream()
                .filter(s -> "FAILED".equalsIgnoreCase(s.getStatus()))
                .count();

        double totalRevenue = subscription.stream()
                .filter(s -> "SUCCESS".equalsIgnoreCase(s.getStatus()))
                .mapToDouble(s -> s.getAmount() == null ? 0.0 : s.getAmount().doubleValue())
                .sum();


        String[] headers = {
                "Payment Date",
                "Amount",
                "Status",
                "Payment Method",
                "Receipt"
        };

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("Billing History");

            // =========================
            // TITLE STYLE
            // =========================

            CellStyle titleStyle = workbook.createCellStyle();

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // ========================
            // Summary Style
            //=========================
            CellStyle summaryStyle = workbook.createCellStyle();

            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);

            summaryStyle.setFont(summaryFont);

            // =========================
            // HEADER STYLE
            // =========================

            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);

            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );

            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // =========================
            // DATA STYLE
            // =========================

            CellStyle dataStyle = workbook.createCellStyle();

            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // =========================
            // DATE STYLE
            // =========================

            //=========================
            // Currency Style
            //==========================
            CellStyle currencyStyle = workbook.createCellStyle();

            currencyStyle.cloneStyleFrom(dataStyle);

            currencyStyle.setDataFormat(
                    workbook.createDataFormat()
                            .getFormat("₹#,##0.00")
            );

            CreationHelper creationHelper =
                    workbook.getCreationHelper();

            CellStyle dateStyle =
                    workbook.createCellStyle();

            dateStyle.cloneStyleFrom(dataStyle);

            dateStyle.setDataFormat(
                    creationHelper
                            .createDataFormat()
                            .getFormat("dd-MMM-yyyy")
            );

            // =========================
            // TITLE ROW
            // =========================

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(25);

            Cell titleCell = titleRow.createCell(0);

            titleCell.setCellValue(
                    "Subscription Billing Report"
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                    new CellRangeAddress(
                            0,
                            0,
                            0,
                            headers.length - 1
                    )
            );

            // =========================
            // SUMMARY ROWS
            // =========================

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy hh:mm a"
                    );

            Row generatedRow = sheet.createRow(1);

            generatedRow.createCell(0)
                    .setCellValue("Generated On");

            generatedRow.createCell(1)
                    .setCellValue(
                            LocalDateTime.now()
                                    .format(formatter)
                    );

            generatedRow.getCell(0)
                    .setCellStyle(summaryStyle);

            Row totalTxnRow = sheet.createRow(2);

            totalTxnRow.createCell(0)
                    .setCellValue("Total Transactions");

            totalTxnRow.createCell(1)
                    .setCellValue(totalTransactions);

            totalTxnRow.getCell(0)
                    .setCellStyle(summaryStyle);

            Row totalPaidRow = sheet.createRow(3);

            totalPaidRow.createCell(0)
                    .setCellValue("Successful Transactions");

            totalPaidRow.createCell(1)
                    .setCellValue(successCount);

            totalPaidRow.getCell(0)
                    .setCellStyle(summaryStyle);

            Row totalDueRow = sheet.createRow(4);

            totalDueRow.createCell(0)
                    .setCellValue("Failed Transactions");

            totalDueRow.createCell(1)
                    .setCellValue(failedCount);

            totalDueRow.getCell(0)
                    .setCellStyle(summaryStyle);

            // Row 5 left blank intentionally

            // =========================
            // HEADER ROW
            // =========================

            Row headerRow = sheet.createRow(6);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }

            // =========================
            // DATA ROWS
            // =========================

            int rowNum = 7;

            for (SubscriptionExportDto subs : subscription) {

                Row row = sheet.createRow(rowNum++);

                //payment date
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(
//                        subs.getDateTime() != null
//                                ? subs.getDateTime()
//                                : null
                        subs.getDateTime() != null ?
                        subs.getDateTime().format(formatter) : null
                );
                cell0.setCellStyle(dataStyle);

                //amount
                Cell cell1 = row.createCell(1);
                cell1.setCellStyle(dataStyle);

                if (subs.getAmount() != null) {
                    cell1.setCellValue(subs.getAmount().doubleValue());
                }

                //status
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(
                        subs.getStatus() != null
                                ? subs.getStatus()
                                : ""
                );
                cell2.setCellStyle(currencyStyle);

                //method
                Cell cell3 = row.createCell(3);

                cell3.setCellValue(
                        subs.getMethod() != null
                        ? subs.getMethod():""
                );
                cell3.setCellStyle(currencyStyle);

                //receipt
                Cell cell4 = row.createCell(4);

                if (subs.getReceipt() != null && !subs.getReceipt().isBlank()) {

                    Hyperlink hyperlink = creationHelper.createHyperlink(
                            HyperlinkType.URL
                    );

                    hyperlink.setAddress(subs.getReceipt());

                    cell4.setCellValue("View Receipt");
                    cell4.setHyperlink(hyperlink);
                    cell4.setCellStyle(dataStyle);
                }
            }

            // =========================
            // FILTERS
            // =========================

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            6,
                            6,
                            0,
                            headers.length - 1
                    )
            );

            // =========================
            // FREEZE HEADER
            // =========================

            sheet.createFreezePane(
                    0,
                    7
            );

            // =========================
            // COLUMN WIDTHS
            // =========================

            sheet.setColumnWidth(0, 6000);   // Date
            sheet.setColumnWidth(1, 6000);   // Amount
            sheet.setColumnWidth(2, 4500);   // Status
            sheet.setColumnWidth(3, 4500);   // Method
            sheet.setColumnWidth(4, 4500);   //  Receipt

            sheet.getRow(6).setHeightInPoints(22);

            workbook.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to export Subscription report",
                    e
            );
        }
    }

    private MemberExportDto mapToMemberExportDto(MemberProjection member) {

        Integer remainingDays = null;

        if (member.getExpiry() != null) {
            remainingDays = (int) ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    member.getExpiry()
            );
        }

        return MemberExportDto.builder()
                .memberId(member.getId())
                .name(member.getName())
                .phone(member.getPhone())
                .email(member.getEmail())
                .membershipName(
                        member.getPlan() != null
                                ? member.getPlan()
                                : ""
                )
                .source(member.getSource())
                .joinedDate(member.getJoined())
                .dueAmount(member.getDueAmount())
                .expiryDate(member.getExpiry())
                .remainingDays(remainingDays)
                .status(member.getIsActive() == 1 ? "Active" : "Inactive")
                .startDate(member.getStartDate())
                .build();
    }

    private PaymentExportDto mapToPaymentExportDto(PaymentProjection payment) {

        return PaymentExportDto.builder()
                .paymentId(payment.getPaymentId())
                .name(payment.getMemberName())
                .date(payment.getPaymentDate())
                .plan(payment.getMembershipName())
                .dueAmount(payment.getDueAmount())
                .method(payment.getMethod())
                .paidAmount(payment.getAmount())
                .build();
    }

    private SubscriptionExportDto mapToSubscriptionExportDto(OwnerPaymentProjection paymentProjection) {
        return SubscriptionExportDto.builder()
                .amount(paymentProjection.getAmount())
                .status(paymentProjection.getStatus())
                .method(paymentProjection.getMethod())
                .dateTime(paymentProjection.getCreatedAt())
                .receipt(paymentProjection.getInvoiceUrl())
                .build();
    }

}

