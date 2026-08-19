package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.MemberShip;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.enums.Payment;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataImportExportService {

   @Autowired
    private MemberRepository memberRepository;

   @Autowired
    private PaymentRepository paymentRepository;

   @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

   @Autowired
    private MemberShipRepository memberShipRepository;

   @Autowired
    private OwnerRepository ownerRepository;

   @Autowired
   private CommonService commonService;

   @Value("${max.pro.member.import.limit}")
    private int maxProMemberImportRowsLimit;

   @Value("${pro.member.import.limit}")
    private int proMemberImportRowsLimit;

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
       Subscription subscription =  commonService.checkSubscriptionOfOwner(Math.toIntExact(ownerId));

        if (subscription!=null) {
            if (!subscription.getName().equals("Max Pro")) {
                return new byte[]{};
            }
        } else {
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
                            member.getSource() != null && !member.getSource().trim().isBlank()
                                    ? member.getSource()
                                    : ""
                    );

                    cell4.setCellStyle(dateStyle);
                }

                Cell cell5 = row.createCell(5);

                if (member.getJoinedDate() != null) {

                    cell5.setCellValue(
                            Date.valueOf(
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
                            Date.valueOf(
                                    member.getExpiryDate()
                            )
                    );

                    cell7.setCellStyle(dateStyle);
                }

                Cell cell8 = row.createCell(8);

                cell8.setCellValue(
                        member.getDueAmount() != null ?
                                member.getDueAmount()
                                : 0
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
            workbook.close();
            out.flush();
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

        Subscription subscription =  commonService.checkSubscriptionOfOwner(Math.toIntExact(ownerId));

        if (subscription!=null) {
            if (!subscription.getName().equals("Max Pro")) {
                return new byte[]{};
            }
        } else {
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
                        payment.getDueAmount() != null ?
                                payment.getDueAmount()
                                : 0
                );
                cell3.setCellStyle(currencyStyle);

                //Date
                Cell cell4 = row.createCell(4);

                cell4.setCellValue(
                        Date.valueOf(
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
            workbook.close();
            out.flush();

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

       Subscription ss =  commonService.checkSubscriptionOfOwner(Math.toIntExact(ownerId));

        if (ss!=null) {
            if (!ss.getName().equals("Max Pro")) {
                return new byte[]{};
            }
        } else {
            throw new RuntimeException("100");
        }

        Payment payment = null;
        if (status != null && status.equals("SUCCESS")) {
            payment = Payment.CAPTURED;
        }
        if (status != null && status.equals("FAILED")) {
            payment = Payment.FAILED;
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
                                ? subs.getMethod() : ""
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
            workbook.close();
            out.flush();

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

   @Transactional(rollbackFor = Exception.class)
    public ImportMemberResponse importMembers(MultipartFile file, Integer ownerId) {
        List<MemberImportDto> members =
                readMembersFromExcel(file);

       Subscription subscription = commonService.checkSubscriptionOfOwner(Math.toIntExact(ownerId));

        if (subscription.getName().strip().equalsIgnoreCase("Max Pro")) {
            if (maxProMemberImportRowsLimit < members.size()) {
                throw new RuntimeException("200");
            }
        } else if (subscription.getName().strip().equalsIgnoreCase("Pro")) {
            if (proMemberImportRowsLimit < members.size()) {
                throw new RuntimeException("150");
            }
        }

        int importedRows = 0;

        int failedRows = 0;

        List<ImportMemberError> errorList =
                new ArrayList<>();
        List<Member> membersToSave = new ArrayList<>();

        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        List<MemberShip> plans =
                memberShipRepository.findAllByGymId(owner.getGym().getId());

        Map<String, MemberShip> planMap =
                plans.stream()
                        .collect(Collectors.toMap(
                                p -> p.getName().trim().toLowerCase(),
                                Function.identity()
                        ));

        for (int i = 0; i < members.size(); i++) {

            MemberImportDto dto = members.get(i);
            List<String> errors =
                    validateMember(dto, planMap);

            if (!errors.isEmpty()) {
                failedRows++;
                errorList.add(
                        ImportMemberError.builder()
                                .rowNumber(dto.getExcelRowNumber())
                                .memberName(dto.getName())
                                .errors(errors)
                                .build()
                );
                continue;
            }
            try {
                MemberShip plan =
                        planMap.get(dto.getPlan().trim().toLowerCase());
                Member member = buildMember(dto, owner, plan);
                membersToSave.add(member);
            } catch (Exception ex) {
                failedRows++;
                errorList.add(
                        ImportMemberError.builder()
                                .rowNumber(dto.getExcelRowNumber())
                                .memberName(dto.getName())
                                .errors(List.of(ex.getMessage()))
                                .build()
                );
            }
        }

        if (failedRows > 0) {

            return ImportMemberResponse.builder()
                    .totalRows(members.size())
                    .importedRows(0)
                    .failedRows(failedRows)
                    .errors(errorList)
                    .build();
        }

        try {

//            memberRepository.saveAll(membersToSave);
            log.info("Members imported successfully");

        } catch (Exception ex) {

            throw new RuntimeException(
                    "not save"
            );
        }

        return ImportMemberResponse.builder()
                .totalRows(members.size())
                .importedRows(membersToSave.size())
                .failedRows(0)
                .errors(Collections.emptyList())
                .build();
    }

    public Workbook validateAndOpenWorkbook(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "203"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null ||
                !fileName.toLowerCase().endsWith(".xlsx")) {

            throw new RuntimeException(
                    "204"
            );
        }

        String contentType = file.getContentType();

        if (!"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                .equals(contentType)) {

            throw new RuntimeException(
                    "Invalid Excel file."
            );
        }

        Workbook workbook;

        try {
            workbook = WorkbookFactory.create(file.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(
                    "202"
            );
        }

        if (workbook.getNumberOfSheets() != 1) {
            throw new RuntimeException(
                    "Invalid template. Only one worksheet is allowed."
            );
        }

        Sheet sheet = workbook.getSheetAt(0);
        findHeaderUsingSheet(sheet);

        return workbook;
    }

    private Row findHeaderUsingSheet(Sheet sheet) {

        String[] expectedHeaders = {
                "Name",
                "Phone",
                "Email",
                "Plan",
                "Joining Date",
                "Start Date",
                "Expiry Date",
                "Due Amount",
                "Address"
        };

        for (Row row : sheet) {

            Cell firstCell = row.getCell(0);

            // Skip empty rows
            if (firstCell == null ||
                    firstCell.getCellType() != CellType.STRING) {
                continue;
            }

            // Look for the header row
            if (!expectedHeaders[0].equalsIgnoreCase(firstCell.getStringCellValue().trim())) {
                continue;
            }

            // Validate total number of columns
            if (row.getLastCellNum() != expectedHeaders.length) {
                throw new RuntimeException(
                        "Invalid template. Expected "
                                + expectedHeaders.length
                                + " columns but found "
                                + row.getLastCellNum() + "."
                );
            }

            // Validate each header
            for (int i = 0; i < expectedHeaders.length; i++) {

                Cell cell = row.getCell(i);

                String actualHeader = cell == null
                        ? ""
                        : cell.getStringCellValue().trim();

                if (!expectedHeaders[i].equalsIgnoreCase(actualHeader)) {

                    throw new RuntimeException(
                            "Invalid template. Expected header '"
                                    + expectedHeaders[i]
                                    + "' at column "
                                    + (i + 1)
                                    + " but found '"
                                    + actualHeader
                                    + "'."
                    );
                }
            }

            // Everything is valid
            return row;
        }

        throw new RuntimeException(
                "Invalid template. Header row not found."
        );
    }

    private List<MemberImportDto> readMembersFromExcel(MultipartFile file) {
        List<MemberImportDto> members = new ArrayList<>();

        try (Workbook workbook = validateAndOpenWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Validate sheet has data
            if (sheet.getPhysicalNumberOfRows() <= 7) {
                throw new RuntimeException(
                        "empty"
                );
            }

            Row headerRow = findHeaderUsingSheet(sheet);
            int headerStartRow = headerRow.getRowNum();

            for (int i = headerStartRow + 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (isRowEmpty(row)) {
                    continue;
                }

                MemberImportDto dto = MemberImportDto.builder()
                        .name(getCellValue(row.getCell(0)))
                        .phone(getCellValue(row.getCell(1)))
                        .email(getCellValue(row.getCell(2)))
                        .plan(getCellValue(row.getCell(3)))
                        .joiningDate(getLocalDate(row.getCell(4)))
                        .startDate(getLocalDate(row.getCell(5)))
                        .expiryDate(getLocalDate(row.getCell(6)))
                        .dueAmount(getInteger(row.getCell(7)))
                        .address(getCellValue(row.getCell(8)))
                        .excelRowNumber(row.getRowNum() + 1)
                        .build();

                members.add(dto);
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "unable"
            );
        }

        return members;
    }

    private boolean isRowEmpty(Row row) {

        if (row == null) {
            return true;
        }

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {

            Cell cell = row.getCell(i);

            if (cell != null &&
                    cell.getCellType() != CellType.BLANK &&
                    !getCellValue(cell).isBlank()) {

                return false;
            }
        }

        return true;
    }

    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }

    private LocalDate getLocalDate(Cell cell) {

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        try {

            // Only treat NUMERIC cells as Excel dates
            if (cell.getCellType() == CellType.NUMERIC &&
                    DateUtil.isCellDateFormatted(cell)) {

                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            String value = getCellValue(cell);

            if (value.isBlank()) {
                return null;
            }

            // yyyy-MM-dd
            try {
                return LocalDate.parse(value);
            } catch (Exception ignored) {
            }

            // dd-MMM-yyyy
            try {
                return LocalDate.parse(
                        value,
                        DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                );
            } catch (Exception ignored) {
            }

            // dd/MM/yyyy
            try {
                return LocalDate.parse(
                        value,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                );
            } catch (Exception ignored) {
            }

            throw new RuntimeException("Invalid date value: " + value);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid date value."
            );
        }
    }

    private Integer getInteger(Cell cell) {

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return 0;
        }

        String value = getCellValue(cell);

        if (value.isBlank()) {
            return 0;
        }

        try {

            // Remove commas if user enters 1,500
            value = value.replace(",", "");

            return Integer.parseInt(value);

        } catch (NumberFormatException e) {

            throw new RuntimeException(
                    "Invalid integer value: " + value
            );
        }
    }

    private int findHeaderRow(Sheet sheet) {

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            String firstCell = getCellValue(row.getCell(0));

            if ("Name".equalsIgnoreCase(firstCell)) {
                return i;
            }
        }

        throw new RuntimeException("Header row not found.");
    }

    private List<String> validateMember(MemberImportDto dto, Map<String, MemberShip> planMap) {
        List<String> errors = new ArrayList<>();

        if (dto.getName() == null ||
                dto.getName().isBlank()) {
            errors.add("Name is required.");
        } else if (dto.getName().length() > 15) {
            errors.add("Name cannot exceed 15 characters.");
        }

        if (dto.getPlan() == null ||
                dto.getPlan().isBlank()) {
            errors.add("Membership plan is required.");
        }

        if (dto.getPlan() != null && !dto.getPlan().isEmpty()) {
            if (!planMap.containsKey(dto.getPlan().trim().toLowerCase())) {
                errors.add("Membership plan does not exist.");
            }
        }

        if (dto.getJoiningDate() == null) {
            errors.add("Joining Date is required.");
        }

        if (dto.getStartDate() == null) {
            errors.add("Start Date is required.");
        }

        if (dto.getExpiryDate() == null) {
            errors.add("Expiry Date is required.");
        } else if (dto.getStartDate() != null &&
                dto.getExpiryDate().isBefore(dto.getStartDate())) {
            errors.add("Expiry date cannot be before start date.");
        }

        if (dto.getDueAmount() == null) {
            errors.add("Due Amount is required.");
        } else if (dto.getDueAmount() < 0) {
            errors.add("Due amount cannot be negative.");
        }

        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            String phone = dto.getPhone().trim();

            if (!phone.matches("^[6-9]\\d{9}$")) {
                errors.add("Phone number must be a valid 10-digit Indian mobile number.");
            }
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            String email = dto.getEmail().trim();

            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                errors.add("Invalid email address.");
            }
        }
        return errors;
    }

    public byte[] downloadMemberImportTemplate() {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("Members Import");

            // =========================================================
            // Fonts
            // =========================================================

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 18);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font instructionTitleFont = workbook.createFont();
            instructionTitleFont.setBold(true);

            // =========================================================
            // Title Style
            // =========================================================

            CellStyle titleStyle = workbook.createCellStyle();

            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // =========================================================
            // Header Style
            // =========================================================

            CellStyle headerStyle = workbook.createCellStyle();

            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_40_PERCENT.getIndex()
            );

            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // =========================================================
            // Normal Cell Style
            // =========================================================

            CellStyle normalStyle = workbook.createCellStyle();

            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            // =========================================================
            // Instruction Title Style
            // =========================================================

            CellStyle instructionTitleStyle =
                    workbook.createCellStyle();

            instructionTitleStyle.setFont(
                    instructionTitleFont
            );

            // =========================================================
            // Date Style
            // =========================================================

            CreationHelper helper =
                    workbook.getCreationHelper();

            CellStyle dateStyle =
                    workbook.createCellStyle();

            dateStyle.cloneStyleFrom(normalStyle);

            dateStyle.setDataFormat(
                    helper.createDataFormat()
                            .getFormat("dd-MM-yyyy")
            );

            // =========================================================
            // Title
            // =========================================================

            Row titleRow = sheet.createRow(0);

            titleRow.setHeightInPoints(28);

            Cell titleCell =
                    titleRow.createCell(0);

            titleCell.setCellValue(
                    "Gym - Members Import Template"
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                    new CellRangeAddress(
                            0,
                            0,
                            0,
                            8
                    )
            );

            // =========================================================
            // Phone Column Format(Text)
            // =========================================================

            CellStyle textStyle = workbook.createCellStyle();

            textStyle.cloneStyleFrom(normalStyle);

            textStyle.setDataFormat(
                    workbook.createDataFormat().getFormat("@")
            );

            // =========================================================
            // Due Amount Format(Numeric)
            // =========================================================

            CellStyle amountStyle = workbook.createCellStyle();

            amountStyle.cloneStyleFrom(normalStyle);

            amountStyle.setDataFormat(
                    workbook.createDataFormat().getFormat("0")
            );

            // =========================================================
            // Instructions Heading
            // =========================================================

            Row instructionHeading =
                    sheet.createRow(2);

            Cell heading =
                    instructionHeading.createCell(0);

            heading.setCellValue(
                    "Instructions"
            );

            heading.setCellStyle(
                    instructionTitleStyle
            );

            // =========================================================
            // Instructions
            // =========================================================

            Font normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 11);

            Font boldRedFont = workbook.createFont();
            boldRedFont.setBold(true);
            boldRedFont.setColor(IndexedColors.RED.getIndex());
            boldRedFont.setFontHeightInPoints((short) 11);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);

            CellStyle instructionHeaderStyle = workbook.createCellStyle();
            Font instructionHeaderFont = workbook.createFont();
            instructionHeaderFont.setBold(true);
            instructionHeaderStyle.setFont(instructionHeaderFont);

            CellStyle instructionStyle = workbook.createCellStyle();
            Font instructionFont = workbook.createFont();
            instructionFont.setBold(false);
            instructionStyle.setFont(instructionFont);

            String[] instructions = {

                    "1. Instructions - Please read instructions carefully before importing Member data.",

                    "2. Do not modify column headers.",

                    "3. Date format must be yyyy-MM-dd.",

                    "4. Membership plan must already exist.",

                    "5. Phone number should be a valid 10 digit Indian mobile number.",

                    "6. Leave Email blank if unavailable.",

                    "7. Due Amount must be zero or positive.",

                    "8. Do not leave required fields blank.",

                    "9. Required Fields - Name, Plan, Joining Date, Start Date, Expiry Date, and Due Amount."

            };

            String[] highlights = {

                    "Please read instructions carefully before importing Member data",
                    "modify",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "Name, Plan, Joining Date, Start Date, Expiry Date, and Due Amount"

            };

            for (int i = 0; i < instructions.length; i++) {

                Row row = sheet.createRow(1 + i);

                Cell cell = row.createCell(0);

                if (highlights[i].isBlank()) {

                    cell.setCellValue(instructions[i]);

                } else {

                    cell.setCellValue(

                            createInstruction(

                                    instructions[i],

                                    highlights[i],

                                    normalFont,

                                    boldRedFont

                            )

                    );

                }

                cell.setCellStyle(

                        i == 0

                                ? instructionHeaderStyle

                                : instructionStyle

                );

            }

            // Blank Row

            sheet.createRow(11);

            // =========================================================
            // Header Row
            // =========================================================

            String[] headers = {

                    "Name",
                    "Phone",
                    "Email",
                    "Plan",
                    "Joining Date",
                    "Start Date",
                    "Expiry Date",
                    "Due Amount",
                    "Address"

            };

            Row headerRow =
                    sheet.createRow(12);

            headerRow.setHeightInPoints(22);

            for (int i = 0; i < headers.length; i++) {

                Cell cell =
                        headerRow.createCell(i);

                cell.setCellValue(
                        headers[i]
                );

                cell.setCellStyle(
                        headerStyle
                );

            }
            // =========================================================
            // SAMPLE DATA
            // =========================================================

            Object[][] sampleData = {
                    {"Ajay J", "9000000000", "ajay0@gmail.com", " Silver", LocalDate.of(2026, 12,
                            10), LocalDate.of(2026, 12, 10), LocalDate.of(2027, 6, 11), 530, "Surat"},
                    {"Ankit K", "9000000001", "ankit1@gmail.com", "Silver", LocalDate.of(2026,
                            10, 14), LocalDate.of(2026, 10, 14),
                            LocalDate.of(2027, 3,
                                    12),
                            1062, "Pune"},
                    {"VikasM", "9000000002", "vikas2@gmail.com", "Gold", LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 5),
                            LocalDate.of(2027, 8, 23), 1830, "Noida"
                    },
                    {"Nitin R", "9000000003", "nitin3@gmail.com", "Silver", LocalDate.of(2026, 12, 19),
                            LocalDate.of(2026, 12, 19), LocalDate.of(2027, 4, 9), 1205, "Indore "},
                    {"KunalV", "9000000004", "kunal4@gmail.com", "Silver", LocalDate.of(2026, 10, 20),
                            LocalDate.of(2026, 10, 20), LocalDate.of(2027, 4, 28), 2366, "Ranchi"},
                    {"Mayank D", "9000000005", "mayank5@gmail.com", "Gold", LocalDate.of(2026, 2, 13),
                            LocalDate.of(2026, 2, 13), LocalDate.of(2026, 9, 18), 757, "Ranchi"},
                    {"KiranV", "9000000006", "kiran6@gmail.com", "Platinum", LocalDate.of(2026, 9, 11),
                            LocalDate.of(2026, 9, 11), LocalDate.of(2027, 4, 21), 2433, "Patna"},
                    {"Sonia V", "9000000007", "sonia7@gmail.com", "Silver", LocalDate.of(2026, 2, 18),
                            LocalDate.of(2026, 2, 18), LocalDate.of(2027, 1, 4), 1230, "Ranchi"},
                    {"PoojaV", "9000000008", "pooja8@gmail.com", "Platinum", LocalDate.of(2026, 8, 3),
                            LocalDate.of(2026, 8, 3), LocalDate.of(2026, 11, 7), 1550, "Patna"},
                    {"Amit T", "9000000009", "amit9@gmail.com", "Gold", LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 8),
                            LocalDate.of(2027, 4, 26), 825, "Pune"},
                    {"Kiran T", "9000000010", "kiran10@gmail.com", "Gold", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1),
                            LocalDate.of(2027, 5, 10), 2414, "Chennai"},
                    {"Kunal J", "9000000011", "kunal11@gmail.com", "Gold", LocalDate.of(2026, 12, 11),
                            LocalDate.of(2026, 12, 11), LocalDate.of(2027, 10, 29), 857, "Ranchi"},
                    {"AnkitP", "9000000012", "ankit12@gmail.com", "Silver", LocalDate.of(2026, 8, 16),
                            LocalDate.of(2026, 8, 16), LocalDate.of(2027, 2, 9), 962, "Surat"},
                    {"Sonia N", "9000000013", "sonia13@gmail.com", "Silver", LocalDate.of(2026, 4, 26),
                            LocalDate.of(2026, 4, 26), LocalDate.of(2026, 12, 28), 2020, "Ranchi"},
                    {"AnkitR", "9000000014", "ankit14@gmail.com", "Platinum", LocalDate.of(2026, 7, 5),
                            LocalDate.of(2026, 7, 5), LocalDate.of(2026, 10, 17), 1423, "Lucknow"},
                    {"RahulJ", "9000000015", "rahul15@gmail.com", "Platinum", LocalDate.of(2026, 1, 18),
                            LocalDate.of(2026, 1, 18), LocalDate.of(2027, 1, 14), 891, "Pune"},
                    {"Ankit D", "9000000016", "ankit16@gmail.com", "Gold", LocalDate.of(2026, 10, 12),
                            LocalDate.of(2026, 10, 12), LocalDate.of(2027, 9, 3), 983, "Bhopal"},
                    {"MayankV", "9000000017", "mayank17@gmail.com", "Platinum", LocalDate.of(2026, 3, 13),
                            LocalDate.of(2026, 3, 13), LocalDate.of(2026, 12, 13), 862, "Indore"},
                    {"Rahul M", "9000000018", "rahul18@gmail.com", "Silver", LocalDate.of(2026, 6, 19),
                            LocalDate.of(2026, 6, 19), LocalDate.of(2027, 5, 3), 2203, "Lucknow"},
                    {"RiyaJ", "9000000019", "riya19@gmail.com", "Platinum", LocalDate.of(2026, 4, 21),
                            LocalDate.of(2026, 4, 21), LocalDate.of(2027, 1, 19), 996, "Jaipur"},
                    {"Ajay K", "9000000020", "ajay20@gmail.com", "Gold", LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 15),
                            LocalDate.of(2027, 7, 25), 1973, "Mumbai"},
                    {"RahulN", "9000000021", "rahul21@gmail.com", "Platinum", LocalDate.of(2026, 10, 1),
                            LocalDate.of(2026, 10, 1), LocalDate.of(2027, 6, 27), 502, "Nashik"},
                    {"Arjun M", "9000000022", "arjun22@gmail.com", "Gold", LocalDate.of(2026, 10, 18),
                            LocalDate.of(2026, 10, 18), LocalDate.of(2027, 3, 31), 1968, "Kolkata"},
                    {"AjayP", "9000000023", "ajay23@gmail.com", "Gold", LocalDate.of(2026, 12, 12),
                            LocalDate.of(2026, 12, 12), LocalDate.of(2027, 6, 5), 1491, "Mumbai"},
                    {"Ajay N", "9000000024", "ajay24@gmail.com", "Gold", LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29),
                            LocalDate.of(2027, 6, 12), 913, "Nashik"},
                    {"KiranK", "9000000025", "kiran25@gmail.com", "Platinum", LocalDate.of(2026, 8, 17),
                            LocalDate.of(2026, 8, 17), LocalDate.of(2027, 5, 26), 2412, "Pune"},
                    {"Amit V", "9000000026", "amit26@gmail.com", "Silver", LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 2),
                            LocalDate.of(2027, 2, 27), 797, "Indore"},
                    {"DeepakM", "9000000027", "deepak27@gmail.com", "Platinum", LocalDate.of(2026, 3, 19),
                            LocalDate.of(2026, 3, 19), LocalDate.of(2026, 9, 25), 1791, "Nashik"},
                    {"Vikas K", "9000000028", "vikas28@gmail.com", "Silver", LocalDate.of(2026, 12, 6),
                            LocalDate.of(2026, 12, 6), LocalDate.of(2027, 8, 22), 1952, "Indore"},
                    {"ArjunV", "9000000029", "arjun29@gmail.com", "Gold", LocalDate.of(2026, 1, 19),
                            LocalDate.of(2026, 1, 19), LocalDate.of(2026, 7, 4), 1466, "Surat"},
                    {"SoniaM", "9000000030", "sonia30@gmail.com", "Platinum", LocalDate.of(2026, 7, 22),
                            LocalDate.of(2026, 7, 22), LocalDate.of(2027, 7, 19), 2439, "Lucknow"},
                    {"Ajay D", "9000000031", "ajay31@gmail.com", "Silver", LocalDate.of(2026, 11, 16),
                            LocalDate.of(2026, 11, 16), LocalDate.of(2027, 9, 21), 2395, "Kolkata"},
                    {"VivekJ", "9000000032", "vivek32@gmail.com", "Silver", LocalDate.of(2026, 4, 12),
                            LocalDate.of(2026, 4, 12), LocalDate.of(2026, 9, 29), 1491, "Bhopal"},
                    {"Ankit N", "9000000033", "ankit33@gmail.com", "Gold", LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 4),
                            LocalDate.of(2026, 11, 27), 640, "Patna"},
                    {"Rahul R", "9000000034", "rahul34@gmail.com", "Silver", LocalDate.of(2026, 12, 4),
                            LocalDate.of(2026, 12, 4), LocalDate.of(2027, 6, 26), 1068, "Pune"},
                    {"Kiran R", "9000000035", "kiran35@gmail.com", "Gold", LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 2),
                            LocalDate.of(2027, 1, 14), 677, "Mumbai"},
                    {"Amit T", "9000000036", "amit36@gmail.com", "Silver", LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 5),
                            LocalDate.of(2026, 9, 5), 1697, "Delhi"},
                    {"NitinP", "9000000037", "nitin37@gmail.com", "Platinum", LocalDate.of(2026, 9, 12),
                            LocalDate.of(2026, 9, 12), LocalDate.of(2027, 5, 16), 1162, "Lucknow"},
                    {"SnehaJ", "9000000038", "sneha38@gmail.com", "Platinum", LocalDate.of(2026, 1, 10),
                            LocalDate.of(2026, 1, 10), LocalDate.of(2026, 11, 12), 646, "Bhopal"},
                    {"VivekP", "9000000039", "vivek39@gmail.com", "Platinum", LocalDate.of(2026, 9, 13),
                            LocalDate.of(2026, 9, 13), LocalDate.of(2027, 6, 17), 1413, "Chennai"},
                    {"Neha P", "9000000040", "neha40@gmail.com", "Gold", LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 15),
                            LocalDate.of(2027, 1, 18), 1457, "Lucknow"},
                    {"SnehaJ", "9000000041", "sneha41@gmail.com", "Platinum", LocalDate.of(2026, 9, 17),
                            LocalDate.of(2026, 9, 17), LocalDate.of(2027, 2, 3), 751, "Patna"},
                    {"Deepak V", "9000000042", "deepak42@gmail.com", "Gold", LocalDate.of(2026, 3, 22),
                            LocalDate.of(2026, 3, 22), LocalDate.of(2026, 11, 5), 1473, "Ranchi"},
                    {"AjayN", "9000000043", "ajay43@gmail.com", "Platinum", LocalDate.of(2026, 5, 22),
                            LocalDate.of(2026, 5, 22), LocalDate.of(2026, 11, 3), 1122, "Delhi"},
                    {"Deepak J", "9000000044", "deepak44@gmail.com", "Gold", LocalDate.of(2026, 9, 25),
                            LocalDate.of(2026, 9, 25), LocalDate.of(2026, 12, 29), 2403, "Chennai"},
                    {"AmitK", "9000000045", "amit45@gmail.com", "Gold", LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 18),
                            LocalDate.of(2027, 4, 9), 1484, "Chennai"},
                    {"Rohan V", "9000000046", "rohan46@gmail.com", "Silver", LocalDate.of(2026, 10, 9),
                            LocalDate.of(2026, 10, 9), LocalDate.of(2027, 10, 1), 2177, "Pune"},
                    {"Riya P", "9000000047", "riya47@gmail.com", "Silver", LocalDate.of(2026, 11, 26),
                            LocalDate.of(2026, 11, 26), LocalDate.of(2027, 6, 20), 572, "Pune"},
                    {"PriyaM", "9000000048", "priya48@gmail.com", "Gold", LocalDate.of(2026, 9, 30),
                            LocalDate.of(2026, 9, 30), LocalDate.of(2027, 8, 18), 1404, "Indore"},
                    {"AnkitD", "9000000049", "ankit49@gmail.com", "Platinum", LocalDate.of(2026, 7, 9),
                            LocalDate.of(2026, 7, 9), LocalDate.of(2027, 3, 6), 1775, "Nashik"},
                    {"Sneha M", "9000000050", "sneha50@gmail.com", "Gold", LocalDate.of(2026, 1, 28),
                            LocalDate.of(2026, 1, 28), LocalDate.of(2027, 1, 23), 1035, "Chennai"},
                    {"Megha V", "9000000051", "megha51@gmail.com", "Silver", LocalDate.of(2026, 9, 19),
                            LocalDate.of(2026, 9, 19), LocalDate.of(2027, 2, 14), 1253, "Nashik"},
                    {"VikasR", "9000000052", "vikas52@gmail.com", "Silver", LocalDate.of(2026, 10, 21),
                            LocalDate.of(2026, 10, 21), LocalDate.of(2027, 10, 9), 894, "Mumbai"},
                    {"Vikas T", "9000000053", "vikas53@gmail.com", "Silver", LocalDate.of(2026, 8, 20),
                            LocalDate.of(2026, 8, 20), LocalDate.of(2027, 2, 1), 2236, "Nashik"},
                    {"SnehaT", "9000000054", "sneha54@gmail.com", "Gold", LocalDate.of(2026, 3, 19),
                            LocalDate.of(2026, 3, 19), LocalDate.of(2027, 1, 29), 1874, "Jaipur"},
                    {"Priya N", "9000000055", "priya55@gmail.com", "Silver", LocalDate.of(2026, 7, 10),
                            LocalDate.of(2026, 7, 10), LocalDate.of(2027, 1, 2), 2236, "Kolkata"},
                    {"VivekR", "9000000056", "vivek56@gmail.com", "Silver", LocalDate.of(2026, 12, 27),
                            LocalDate.of(2026, 12, 27), LocalDate.of(2027, 6, 5), 986, "Surat"},
                    {"Sonia V", "9000000057", "sonia57@gmail.com", "Silver", LocalDate.of(2026, 11, 18),
                            LocalDate.of(2026, 11, 18), LocalDate.of(2027, 5, 30), 2242, "Patna"},
                    {"NitinT", "9000000058", "nitin58@gmail.com", "Gold", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                            LocalDate.of(2026, 12, 24), 538, "Noida"},
                    {"Neha M", "9000000059", "neha59@gmail.com", "Gold", LocalDate.of(2026, 7, 27), LocalDate.of(2026, 7, 27),
                            LocalDate.of(2027, 5, 28), 1796, "Noida"},
                    {"Vikas V", "9000000060", "vikas60@gmail.com", "Silver", LocalDate.of(2026, 1, 7),
                            LocalDate.of(2026, 1, 7), LocalDate.of(2026, 10, 19), 1815, "Mumbai"},
                    {"VikasT", "9000000061", "vikas61@gmail.com", "Platinum", LocalDate.of(2026, 12, 21),
                            LocalDate.of(2026, 12, 21), LocalDate.of(2027, 9, 10), 1012, "Lucknow"},
                    {"RohanV", "9000000062", "rohan62@gmail.com", "Gold", LocalDate.of(2026, 10, 26),
                            LocalDate.of(2026, 10, 26), LocalDate.of(2027, 3, 25), 1859, "Bhopal"},
                    {"Sneha M", "9000000063", "sneha63@gmail.com", "Gold", LocalDate.of(2026, 3, 31),
                            LocalDate.of(2026, 3, 31), LocalDate.of(2026, 10, 29), 1909, "Patna"},
                    {"Kunal R", "9000000064", "kunal64@gmail.com", "Silver", LocalDate.of(2026, 7, 20),
                            LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 4), 1692, "Patna"},
                    {"RiyaN", "9000000065", "riya65@gmail.com", "Platinum", LocalDate.of(2026, 12, 28),
                            LocalDate.of(2026, 12, 28), LocalDate.of(2027, 10, 16), 1241, "Surat"},
                    {"Sonia R", "9000000066", "sonia66@gmail.com", "Gold", LocalDate.of(2026, 7, 11),
                            LocalDate.of(2026, 7, 11), LocalDate.of(2027, 4, 3), 1601, "Jaipur"},
                    {"RohanR", "9000000067", "rohan67@gmail.com", "Platinum", LocalDate.of(2026, 2, 12),
                            LocalDate.of(2026, 2, 12), LocalDate.of(2027, 1, 1), 2241, "Noida"},
                    {"MayankS", "9000000068", "mayank68@gmail.com", "Silver", LocalDate.of(2026, 9, 18),
                            LocalDate.of(2026, 9, 18), LocalDate.of(2027, 4, 15), 2132, "Bhopal"},
                    {"Rohan T", "9000000069", "rohan69@gmail.com", "Gold", LocalDate.of(2026, 9, 26),
                            LocalDate.of(2026, 9, 26), LocalDate.of(2027, 8, 14), 1251, "Nagpur"},
                    {"Riya M", "9000000070", "riya70@gmail.com", "Silver", LocalDate.of(2026, 10, 28),
                            LocalDate.of(2026, 10, 28), LocalDate.of(2027, 3, 18), 2326, "Lucknow"},
                    {"VivekJ", "9000000071", "vivek71@gmail.com", "Platinum", LocalDate.of(2026, 8, 27),
                            LocalDate.of(2026, 8, 27), LocalDate.of(2027, 1, 20), 2464, "Delhi"},
                    {"Ajay V", "9000000072", "ajay72@gmail.com", "Gold", LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 20),
                            LocalDate.of(2026, 9, 17), 553, "Bhopal"},
                    {"Amit J", "9000000073", "amit73@gmail.com", "Silver", LocalDate.of(2026, 7, 24),
                            LocalDate.of(2026, 7, 24), LocalDate.of(2027, 1, 9), 1573, "Nashik"},
                    {"MayankM", "9000000074", "mayank74@gmail.com", "Platinum", LocalDate.of(2026, 11, 3),
                            LocalDate.of(2026, 11, 3), LocalDate.of(2027, 6, 23), 633, "Ranchi"},
                    {"ArjunN", "9000000075", "arjun75@gmail.com", "Platinum", LocalDate.of(2026, 7, 15),
                            LocalDate.of(2026, 7, 15), LocalDate.of(2027, 6, 11), 1789, "Mumbai"},
                    {"Kiran J", "9000000076", "kiran76@gmail.com", "Gold", LocalDate.of(2026, 2, 18),
                            LocalDate.of(2026, 2, 18), LocalDate.of(2026, 10, 2), 535, "Nagpur"},
                    {"AnkitV", "9000000077", "ankit77@gmail.com", "Platinum", LocalDate.of(2026, 10, 8),
                            LocalDate.of(2026, 10, 8), LocalDate.of(2027, 4, 21), 1196, "Ranchi"},
                    {"Priya T", "9000000078", "priya78@gmail.com", "Gold", LocalDate.of(2026, 3, 16),
                            LocalDate.of(2026, 3, 16), LocalDate.of(2027, 2, 15), 2482, "Chennai"},
                    {"Sneha N", "9000000079", "sneha79@gmail.com", "Gold", LocalDate.of(2026, 9, 16),
                            LocalDate.of(2026, 9, 16), LocalDate.of(2027, 7, 14), 686, "Indore"},
                    {"Rahul M", "9000000080", "rahul80@gmail.com", "Silver", LocalDate.of(2026, 8, 11),
                            LocalDate.of(2026, 8, 11), LocalDate.of(2027, 1, 4), 974, "Noida"},
                    {"PriyaN", "9000000081", "priya81@gmail.com", "Platinum", LocalDate.of(2026, 6, 9),
                            LocalDate.of(2026, 6, 9), LocalDate.of(2027, 4, 12), 1178, "Chennai"},
                    {"Vivek M", "9000000082", "vivek82@gmail.com", "Gold", LocalDate.of(2026, 9, 27),
                            LocalDate.of(2026, 9, 27), LocalDate.of(2027, 4, 6), 676, "Nashik"},
                    {"Sonia D", "9000000083", "sonia83@gmail.com", "Gold", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                            LocalDate.of(2026, 8, 31), 1983, "Noida"},
                    {"Sneha R", "9000000084", "sneha84@gmail.com", "Gold", LocalDate.of(2026, 4, 24),
                            LocalDate.of(2026, 4, 24), LocalDate.of(2027, 2, 17), 1402, "Jaipur"},
                    {"DeepakS", "9000000085", "deepak85@gmail.com", "Silver", LocalDate.of(2026, 12, 9),
                            LocalDate.of(2026, 12, 9), LocalDate.of(2027, 8, 21), 1951, "Mumbai"},
                    {"Riya J", "9000000086", "riya86@gmail.com", "Gold", LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8),
                            LocalDate.of(2027, 1, 22), 1761, "Jaipur"},
                    {"DeepakS", "9000000087", "deepak87@gmail.com", "Platinum", LocalDate.of(2026, 3, 5),
                            LocalDate.of(2026, 3, 5), LocalDate.of(2026, 9, 16), 738, "Indore"},
                    {"Sonia J", "9000000088", "sonia88@gmail.com", "Gold", LocalDate.of(2026, 6, 7), LocalDate.of(2026, 6, 7),
                            LocalDate.of(2026, 12, 18), 2181, "Mumbai"},
                    {"VikasJ", "9000000089", "vikas89@gmail.com", "Platinum", LocalDate.of(2026, 6, 3),
                            LocalDate.of(2026, 6, 3), LocalDate.of(2026, 9, 11), 846, "Ranchi"},
                    {"Kunal K", "9000000090", "kunal90@gmail.com", "Gold", LocalDate.of(2026, 10, 4),
                            LocalDate.of(2026, 10, 4), LocalDate.of(2027, 5, 22), 2369, "Mumbai"},
                    {"AmitN", "9000000091", "amit91@gmail.com", "Platinum", LocalDate.of(2026, 11, 29),
                            LocalDate.of(2026, 11, 29), LocalDate.of(2027, 10, 21), 2088, "Jaipur"},
                    {"AnkitS", "9000000092", "ankit92@gmail.com", "Gold", LocalDate.of(2026, 11, 4),
                            LocalDate.of(2026, 11, 4), LocalDate.of(2027, 5, 9), 1094, "Surat"},
                    {"Megha S", "9000000093", "megha93@gmail.com", "Gold", LocalDate.of(2026, 5, 26),
                            LocalDate.of(2026, 5, 26), LocalDate.of(2026, 9, 12), 1161, "Noida"},
                    {"Amit V", "9000000094", "amit94@gmail.com", "Gold", LocalDate.of(2026, 2, 13), LocalDate.of(2026, 2, 13),
                            LocalDate.of(2026, 12, 24), 1732, "Patna"},
                    {"Rohan P", "9000000095", "rohan95@gmail.com", "Silver", LocalDate.of(2026, 5, 7),
                            LocalDate.of(2026, 5, 7), LocalDate.of(2027, 4, 25), 2433, "Patna"},
                    {"Rahul S", "9000000096", "rahul96@gmail.com", "Gold", LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 8),
                            LocalDate.of(2027, 5, 13), 2112, "Chennai"},
                    {"Amit D", "9000000097", "amit97@gmail.com", "Silver", LocalDate.of(2026, 1, 26),
                            LocalDate.of(2026, 1, 26), LocalDate.of(2026, 4, 29), 2344, "Mumbai"},
                    {"Riya T", "9000000098", "riya98@gmail.com", "Silver", LocalDate.of(2026, 5, 23),
                            LocalDate.of(2026, 5, 23), LocalDate.of(2026, 11, 4), 1214, "Patna"},
                    {"VivekS", "9000000099", "vivek99@gmail.com", "Platinum", LocalDate.of(2026, 9, 30),
                            LocalDate.of(2026, 9, 30), LocalDate.of(2027, 1, 10), 2388, "Mumbai"},
                    {"Neha V", "9000000100", "neha100@gmail.com", "Gold", LocalDate.of(2026, 8, 30), LocalDate.of(2026, 8, 30),
                            LocalDate.of(2027, 5, 17), 2066, "Indore"},
                    {"VivekJ", "9000000101", "vivek101@gmail.com", "Platinum", LocalDate.of(2026, 11, 7),
                            LocalDate.of(2026, 11, 7), LocalDate.of(2027, 8, 12), 2385, "Kolkata"},
                    {"NitinP", "9000000102", "nitin102@gmail.com", "Platinum", LocalDate.of(2026, 7, 7),
                            LocalDate.of(2026, 7, 7), LocalDate.of(2026, 12, 17), 1035, "Kolkata"},
                    {"Ajay R", "9000000103", "ajay103@gmail.com", "Gold", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 7),
                            LocalDate.of(2027, 8, 24), 1797, "Jaipur"},
                    {"Neha T", "9000000104", "neha104@gmail.com", "Gold", LocalDate.of(2026, 6, 14), LocalDate.of(2026, 6, 14),
                            LocalDate.of(2027, 1, 4), 1361, "Nagpur"},
                    {"DeepakP", "9000000105", "deepak105@gmail.com", "Silver", LocalDate.of(2026, 3, 3),
                            LocalDate.of(2026, 3, 3), LocalDate.of(2027, 2, 26), 927, "Noida"},
                    {"Priya D", "9000000106", "priya106@gmail.com", "Silver", LocalDate.of(2026, 2, 2),
                            LocalDate.of(2026, 2, 2), LocalDate.of(2026, 9, 15), 831, "Bhopal"},
                    {"Amit K", "9000000107", "amit107@gmail.com", "Platinum", LocalDate.of(2026, 5, 20),
                            LocalDate.of(2026, 5, 20), LocalDate.of(2026, 10, 13), 525, "Indore"},
                    {"DeepakV", "9000000108", "deepak108@gmail.com", "Gold", LocalDate.of(2026, 3, 2),
                            LocalDate.of(2026, 3, 2), LocalDate.of(2026, 9, 20), 2499, "Patna"},
                    {"Megha R", "9000000109", "megha109@gmail.com", "Silver", LocalDate.of(2026, 12, 18),
                            LocalDate.of(2026, 12, 18), LocalDate.of(2027, 7, 16), 1375, "Chennai"},
                    {"RiyaK", "9000000110", "riya110@gmail.com", "Gold", LocalDate.of(2026, 2, 15), LocalDate.of(2026, 2, 15),
                            LocalDate.of(2026, 7, 6), 2043, "Pune"},
                    {"Ankit R", "9000000111", "ankit111@gmail.com", "Gold", LocalDate.of(2026, 12, 23),
                            LocalDate.of(2026, 12, 23), LocalDate.of(2027, 7, 5), 1876, "Delhi"},
                    {"AjayV", "9000000112", "ajay112@gmail.com", "Gold", LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 8),
                            LocalDate.of(2027, 3, 4), 1953, "Surat"},
                    {"Nitin N", "9000000113", "nitin113@gmail.com", "Silver", LocalDate.of(2026, 7, 22),
                            LocalDate.of(2026, 7, 22), LocalDate.of(2027, 4, 24), 1621, "Surat"},
                    {"SnehaP", "9000000114", "sneha114@gmail.com", "Silver", LocalDate.of(2026, 8, 21),
                            LocalDate.of(2026, 8, 21), LocalDate.of(2027, 4, 10), 2027, "Patna"},
                    {"Neha V", "9000000115", "neha115@gmail.com", "Gold", LocalDate.of(2026, 4, 8), LocalDate.of(2026, 4, 8),
                            LocalDate.of(2027, 3, 10), 1295, "Jaipur"},
                    {"Nitin N", "9000000116", "nitin116@gmail.com", "Gold", LocalDate.of(2026, 1, 14),
                            LocalDate.of(2026, 1, 14), LocalDate.of(2026, 9, 17), 2254, "Delhi"},
                    {"VikasK", "9000000117", "vikas117@gmail.com", "Gold", LocalDate.of(2026, 5, 21),
                            LocalDate.of(2026, 5, 21), LocalDate.of(2026, 10, 29), 2234, "Bhopal"},
                    {"Amit T", "9000000118", "amit118@gmail.com", "Silver", LocalDate.of(2026, 12, 31),
                            LocalDate.of(2026, 12, 31), LocalDate.of(2027, 8, 2), 2465, "Nashik"},
                    {"AnkitK", "9000000119", "ankit119@gmail.com", "Platinum", LocalDate.of(2026, 10, 21),
                            LocalDate.of(2026, 10, 21), LocalDate.of(2027, 6, 2), 982, "Noida"},
                    {"SoniaS", "9000000120", "sonia120@gmail.com", "Platinum", LocalDate.of(2026, 3, 4),
                            LocalDate.of(2026, 3, 4), LocalDate.of(2027, 2, 15), 2236, "Lucknow"},
                    {"NitinN", "9000000121", "nitin121@gmail.com", "Platinum", LocalDate.of(2026, 11, 23),
                            LocalDate.of(2026, 11, 23), LocalDate.of(2027, 9, 17), 1165, "Nashik"},
                    {"Sneha V", "9000000122", "sneha122@gmail.com", "Gold", LocalDate.of(2026, 3, 11),
                            LocalDate.of(2026, 3, 11), LocalDate.of(2026, 12, 2), 1208, "Delhi"},
                    {"ArjunS", "9000000123", "arjun123@gmail.com", "Gold", LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 6),
                            LocalDate.of(2027, 2, 23), 1971, "Patna"},
                    {"Neha T", "9000000124", "neha124@gmail.com", "Silver", LocalDate.of(2026, 7, 28),
                            LocalDate.of(2026, 7, 28), LocalDate.of(2027, 4, 23), 1071, "Jaipur"},
                    {"SoniaT", "9000000125", "sonia125@gmail.com", "Gold", LocalDate.of(2026, 5, 17),
                            LocalDate.of(2026, 5, 17), LocalDate.of(2027, 5, 8), 2347, "Pune"},
                    {"Rohan V", "9000000126", "rohan126@gmail.com", "Silver", LocalDate.of(2026, 2, 25),
                            LocalDate.of(2026, 2, 25), LocalDate.of(2026, 7, 26), 1948, "Patna"},
                    {"DeepakS", "9000000127", "deepak127@gmail.com", "Platinum", LocalDate.of(2026, 10, 10),
                            LocalDate.of(2026, 10, 10), LocalDate.of(2027, 7, 3), 1228, "Bhopal"},
                    {"Ankit M", "9000000128", "ankit128@gmail.com", "Silver", LocalDate.of(2026, 5, 6),
                            LocalDate.of(2026, 5, 6), LocalDate.of(2026, 9, 13), 504, "Indore"},
                    {"Priya V", "9000000129", "priya129@gmail.com", "Silver", LocalDate.of(2026, 11, 29),
                            LocalDate.of(2026, 11, 29), LocalDate.of(2027, 6, 21), 1273, "Lucknow"},
                    {"AnkitN", "9000000130", "ankit130@gmail.com", "Silver", LocalDate.of(2026, 12, 30),
                            LocalDate.of(2026, 12, 30), LocalDate.of(2027, 11, 20), 1902, "Patna"},
                    {"Sneha P", "9000000131", "sneha131@gmail.com", "Silver", LocalDate.of(2026, 9, 17),
                            LocalDate.of(2026, 9, 17), LocalDate.of(2027, 5, 26), 2369, "Nagpur"},
                    {"AmitT", "9000000132", "amit132@gmail.com", "Silver", LocalDate.of(2026, 10, 4),
                            LocalDate.of(2026, 10, 4), LocalDate.of(2027, 8, 1), 2408, "Kolkata"},
                    {"Arjun V", "9000000133", "arjun133@gmail.com", "Silver", LocalDate.of(2026, 10, 12),
                            LocalDate.of(2026, 10, 12), LocalDate.of(2027, 3, 25), 1980, "Indore"},
                    {"AnkitV", "9000000134", "ankit134@gmail.com", "Platinum", LocalDate.of(2026, 1, 31),
                            LocalDate.of(2026, 1, 31), LocalDate.of(2026, 5, 4), 1494, "Indore"},
                    {"SoniaV", "9000000135", "sonia135@gmail.com", "Platinum", LocalDate.of(2026, 11, 12),
                            LocalDate.of(2026, 11, 12), LocalDate.of(2027, 10, 22), 872, "Lucknow"},
                    {"KunalN", "9000000136", "kunal136@gmail.com", "Gold", LocalDate.of(2026, 1, 29),
                            LocalDate.of(2026, 1, 29), LocalDate.of(2026, 6, 5), 1821, "Chennai"},
                    {"Vivek R", "9000000137", "vivek137@gmail.com", "Gold", LocalDate.of(2026, 8, 26),
                            LocalDate.of(2026, 8, 26), LocalDate.of(2027, 3, 4), 1241, "Nashik"},
                    {"RahulP", "9000000138", "rahul138@gmail.com", "Platinum", LocalDate.of(2026, 9, 12),
                            LocalDate.of(2026, 9, 12), LocalDate.of(2027, 7, 19), 2242, "Nagpur"},
                    {"Vikas N", "9000000139", "vikas139@gmail.com", "Gold", LocalDate.of(2026, 1, 13),
                            LocalDate.of(2026, 1, 13), LocalDate.of(2026, 5, 10), 788, "Mumbai"},
                    {"NitinP", "9000000140", "nitin140@gmail.com", "Platinum", LocalDate.of(2026, 12, 30),
                            LocalDate.of(2026, 12, 30), LocalDate.of(2027, 7, 4), 1465, "Patna"},
                    {"NitinJ", "9000000141", "nitin141@gmail.com", "Platinum", LocalDate.of(2026, 11, 11),
                            LocalDate.of(2026, 11, 11), LocalDate.of(2027, 10, 17), 1428, "Noida"},
                    {"Neha K", "9000000142", "neha142@gmail.com", "Gold", LocalDate.of(2026, 3, 19), LocalDate.of(2026, 3, 19),
                            LocalDate.of(2027, 2, 12), 1620, "Lucknow"},
                    {"KunalJ", "9000000143", "kunal143@gmail.com", "Platinum", LocalDate.of(2026, 4, 10),
                            LocalDate.of(2026, 4, 10), LocalDate.of(2026, 9, 24), 619, "Chennai"},
                    {"KunalS", "9000000144", "kunal144@gmail.com", "Platinum", LocalDate.of(2026, 12, 11),
                            LocalDate.of(2026, 12, 11), LocalDate.of(2027, 4, 17), 1132, "Mumbai"},
                    {"Mayank D", "9000000145", "mayank145@gmail.com", "Gold", LocalDate.of(2026, 11, 13),
                            LocalDate.of(2026, 11, 13), LocalDate.of(2027, 9, 18), 694, "Chennai"},
                    {"AjayD", "9000000146", "ajay146@gmail.com", "Gold", LocalDate.of(2026, 6, 6), LocalDate.of(2026, 6, 6),
                            LocalDate.of(2027, 3, 25), 647, "Kolkata"},
                    {"AnkitS", "9000000147", "ankit147@gmail.com", "Platinum", LocalDate.of(2026, 10, 25),
                            LocalDate.of(2026, 10, 25), LocalDate.of(2027, 9, 16), 1935, "Indore"},
                    {"MayankD", "9000000148", "mayank148@gmail.com", "Silver", LocalDate.of(2026, 9, 14),
                            LocalDate.of(2026, 9, 14), LocalDate.of(2027, 5, 21), 1861, "Patna"},
                    {"Riya N", "9000000149", "riya149@gmail.com", "Silver", LocalDate.of(2026, 9, 21),
                            LocalDate.of(2026, 9, 21), LocalDate.of(2027, 8, 23), 1571, "Mumbai"},
                    {"VivekJ", "9000000150", "vivek150@gmail.com", "Gold", LocalDate.of(2026, 8, 14),
                            LocalDate.of(2026, 8, 14), LocalDate.of(2027, 5, 10), 2052, "Bhopal"},
                    {"Megha R", "9000000151", "megha151@gmail.com", "Silver", LocalDate.of(2026, 3, 1),
                            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 10, 1), 2277, "Jaipur"},
                    {"Sneha D", "9000000152", "sneha152@gmail.com", "Gold", LocalDate.of(2026, 11, 27),
                            LocalDate.of(2026, 11, 27), LocalDate.of(2027, 9, 11), 818, "Patna"},
                    {"SoniaM", "9000000153", "sonia153@gmail.com", "Gold", LocalDate.of(2026, 11, 3),
                            LocalDate.of(2026, 11, 3), LocalDate.of(2027, 2, 24), 1405, "Noida"},
                    {"Sneha D", "9000000154", "sneha154@gmail.com", "Silver", LocalDate.of(2026, 3, 26),
                            LocalDate.of(2026, 3, 26), LocalDate.of(2027, 1, 25), 779, "Nashik"},
                    {"VivekS", "9000000155", "vivek155@gmail.com", "Silver", LocalDate.of(2026, 7, 17),
                            LocalDate.of(2026, 7, 17), LocalDate.of(2027, 2, 19), 2239, "Ranchi"},
                    {"Ajay V", "9000000156", "ajay156@gmail.com", "Platinum", LocalDate.of(2026, 11, 10),
                            LocalDate.of(2026, 11, 10), LocalDate.of(2027, 2, 11), 953, "Nashik"},
                    {"MeghaD", "9000000157", "megha157@gmail.com", "Platinum", LocalDate.of(2026, 3, 9),
                            LocalDate.of(2026, 3, 9), LocalDate.of(2027, 2, 6), 982, "Lucknow"},
                    {"Neha D", "9000000158", "neha158@gmail.com", "Silver", LocalDate.of(2026, 7, 14),
                            LocalDate.of(2026, 7, 14), LocalDate.of(2027, 1, 24), 743, "Pune"},
                    {"Amit N", "9000000159", "amit159@gmail.com", "Gold", LocalDate.of(2026, 12, 9), LocalDate.of(2026, 12, 9),
                            LocalDate.of(2027, 4, 10), 1334, "Patna"},
                    {"Ajay J", "9000000160", "ajay160@gmail.com", "Platinum", LocalDate.of(2026, 12, 23),
                            LocalDate.of(2026, 12, 23), LocalDate.of(2027, 10, 9), 2029, "Nagpur"},
                    {"AnkitP", "9000000161", "ankit161@gmail.com", "Gold", LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 3),
                            LocalDate.of(2027, 1, 13), 953, "Noida"},
                    {"NitinS", "9000000162", "nitin162@gmail.com", "Platinum", LocalDate.of(2026, 3, 30),
                            LocalDate.of(2026, 3, 30), LocalDate.of(2027, 1, 17), 2466, "Ranchi"},
                    {"Neha M", "9000000163", "neha163@gmail.com", "Gold", LocalDate.of(2026, 1, 9), LocalDate.of(2026, 1, 9),
                            LocalDate.of(2026, 7, 28), 1598, "Jaipur"},
                    {"Ankit D", "9000000164", "ankit164@gmail.com", "Silver", LocalDate.of(2026, 9, 24),
                            LocalDate.of(2026, 9, 24), LocalDate.of(2027, 4, 23), 540, "Delhi"},
                    {"Vivek D", "9000000165", "vivek165@gmail.com", "Gold", LocalDate.of(2026, 3, 18),
                            LocalDate.of(2026, 3, 18), LocalDate.of(2027, 2, 24), 2246, "Delhi"},
                    {"RohanK", "9000000166", "rohan166@gmail.com", "Platinum", LocalDate.of(2026, 10, 22),
                            LocalDate.of(2026, 10, 22), LocalDate.of(2027, 8, 1), 845, "Bhopal"},
                    {"Deepak K", "9000000167", "deepak167@gmail.com", "Gold", LocalDate.of(2026, 8, 18),
                            LocalDate.of(2026, 8, 18), LocalDate.of(2027, 4, 6), 1193, "Lucknow"},
                    {"VikasN", "9000000168", "vikas168@gmail.com", "Silver", LocalDate.of(2026, 11, 20),
                            LocalDate.of(2026, 11, 20), LocalDate.of(2027, 9, 18), 1674, "Nagpur"},
                    {"Sonia T", "9000000169", "sonia169@gmail.com", "Gold", LocalDate.of(2026, 10, 31),
                            LocalDate.of(2026, 10, 31), LocalDate.of(2027, 8, 17), 1387, "Noida"},
                    {"MayankJ", "9000000170", "mayank170@gmail.com", "Platinum", LocalDate.of(2026, 3, 14),
                            LocalDate.of(2026, 3, 14), LocalDate.of(2026, 12, 19), 541, "Nagpur"},
                    {"Neha T", "9000000171", "neha171@gmail.com", "Gold", LocalDate.of(2026, 6, 26), LocalDate.of(2026, 6, 26),
                            LocalDate.of(2027, 3, 12), 1566, "Patna"},
                    {"Nitin K", "9000000172", "nitin172@gmail.com", "Silver", LocalDate.of(2026, 7, 18),
                            LocalDate.of(2026, 7, 18), LocalDate.of(2027, 3, 31), 1111, "Kolkata"},
                    {"VivekR", "9000000173", "vivek173@gmail.com", "Gold", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1),
                            LocalDate.of(2027, 4, 12), 830, "Jaipur"},
                    {"RahulS", "9000000174", "rahul174@gmail.com", "Platinum", LocalDate.of(2026, 3, 5),
                            LocalDate.of(2026, 3, 5), LocalDate.of(2026, 6, 29), 2055, "Jaipur"},
                    {"Mayank K", "9000000175", "mayank175@gmail.com", "Gold", LocalDate.of(2026, 10, 2),
                            LocalDate.of(2026, 10, 2), LocalDate.of(2027, 9, 23), 1346, "Noida"},
                    {"PoojaS", "9000000176", "pooja176@gmail.com", "Silver", LocalDate.of(2026, 6, 6),
                            LocalDate.of(2026, 6, 6), LocalDate.of(2026, 9, 22), 661, "Indore"},
                    {"Priya N", "9000000177", "priya177@gmail.com", "Gold", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1),
                            LocalDate.of(2026, 10, 14), 951, "Jaipur"},
                    {"Amit V", "9000000178", "amit178@gmail.com", "Platinum", LocalDate.of(2026, 12, 25),
                            LocalDate.of(2026, 12, 25), LocalDate.of(2027, 8, 11), 1128, "Noida"},
                    {"RiyaJ", "9000000179", "riya179@gmail.com", "Silver", LocalDate.of(2026, 8, 20),
                            LocalDate.of(2026, 8, 20), LocalDate.of(2027, 3, 4), 623, "Pune"},
                    {"Amit R", "9000000180", "amit180@gmail.com", "Silver", LocalDate.of(2026, 5, 10),
                            LocalDate.of(2026, 5, 10), LocalDate.of(2027, 2, 7), 2150, "Surat"},
                    {"Nitin M", "9000000181", "nitin181@gmail.com", "Gold", LocalDate.of(2026, 11, 6),
                            LocalDate.of(2026, 11, 6), LocalDate.of(2027, 4, 12), 1246, "Mumbai"},
                    {"VivekN", "9000000182", "vivek182@gmail.com", "Gold", LocalDate.of(2026, 7, 24),
                            LocalDate.of(2026, 7, 24), LocalDate.of(2026, 12, 17), 666, "Lucknow"},
                    {"Arjun P", "9000000183", "arjun183@gmail.com", "Silver", LocalDate.of(2026, 9, 11),
                            LocalDate.of(2026, 9, 11), LocalDate.of(2027, 1, 4), 2170, "Pune"},
                    {"Ajay K", "9000000184", "ajay184@gmail.com", "Gold", LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 8),
                            LocalDate.of(2027, 4, 22), 1265, "Noida"},
                    {"MayankR", "9000000185", "mayank185@gmail.com", "Silver", LocalDate.of(2026, 12, 29),
                            LocalDate.of(2026, 12, 29), LocalDate.of(2027, 9, 13), 1520, "Surat"},
                    {"Priya P", "9000000186", "priya186@gmail.com", "Silver", LocalDate.of(2026, 10, 15),
                            LocalDate.of(2026, 10, 15), LocalDate.of(2027, 1, 31), 1465, "Nagpur"},
                    {"NitinT", "9000000187", "nitin187@gmail.com", "Silver", LocalDate.of(2026, 12, 31),
                            LocalDate.of(2026, 12, 31), LocalDate.of(2027, 9, 12), 1676, "Jaipur"},
                    {"Pooja P", "9000000188", "pooja188@gmail.com", "Gold", LocalDate.of(2026, 3, 31),
                            LocalDate.of(2026, 3, 31), LocalDate.of(2026, 11, 28), 1866, "Patna"},
                    {"AnkitK", "9000000189", "ankit189@gmail.com", "Silver", LocalDate.of(2026, 9, 21),
                            LocalDate.of(2026, 9, 21), LocalDate.of(2027, 7, 15), 2125, "Surat"},
                    {"SnehaJ", "9000000190", "sneha190@gmail.com", "Platinum", LocalDate.of(2026, 12, 30),
                            LocalDate.of(2026, 12, 30), LocalDate.of(2027, 6, 20), 1373, "Indore"},
                    {"RahulD", "9000000191", "rahul191@gmail.com", "Platinum", LocalDate.of(2026, 8, 28),
                            LocalDate.of(2026, 8, 28), LocalDate.of(2027, 3, 18), 1925, "Patna"},
                    {"Amit D", "9000000192", "amit192@gmail.com", "Platinum", LocalDate.of(2026, 1, 29),
                            LocalDate.of(2026, 1, 29), LocalDate.of(2026, 11, 14), 1047, "Pune"},
                    {"NehaD", "9000000193", "neha193@gmail.com", "Platinum", LocalDate.of(2026, 7, 6),
                            LocalDate.of(2026, 7, 6), LocalDate.of(2026, 10, 15), 2482, "Delhi"},
                    {"Rahul R", "9000000194", "rahul194@gmail.com", "Gold", LocalDate.of(2026, 1, 22),
                            LocalDate.of(2026, 1, 22), LocalDate.of(2026, 5, 31), 865, "Delhi"},
                    {"Neha M", "9000000195", "neha195@gmail.com", "Platinum", LocalDate.of(2026, 8, 3),
                            LocalDate.of(2026, 8, 3), LocalDate.of(2027, 1, 23), 603, "Indore"},
                    {"RohanS", "9000000196", "rohan196@gmail.com", "Platinum", LocalDate.of(2026, 5, 30),
                            LocalDate.of(2026, 5, 30), LocalDate.of(2026, 12, 20), 792, "Ranchi"},
                    {"Megha R", "9000000197", "megha197@gmail.com", "Gold", LocalDate.of(2026, 1, 13),
                            LocalDate.of(2026, 1, 13), LocalDate.of(2026, 5, 30), 1168, "Patna"},
                    {"AnkitT", "9000000198", "ankit198@gmail.com", "Platinum", LocalDate.of(2026, 11, 6),
                            LocalDate.of(2026, 11, 6), LocalDate.of(2027, 10, 2), 1090, "Kolkata"},
                    {"MeghaT", "9000000199", "megha199@gmail.com", "Platinum", LocalDate.of(2026, 8, 8),
                            LocalDate.of(2026, 8, 8), LocalDate.of(2027, 3, 25), 1184, "Indore"},
                    {"KiranT", "9000000200", "kiran200@gmail.com", "Platinum", LocalDate.of(2026, 7, 6),
                            LocalDate.of(2026, 7, 6), LocalDate.of(2027, 2, 9), 2104, "Jaipur"},
                    {"Kiran D", "9000000201", "kiran201@gmail.com", "Silver", LocalDate.of(2026, 10, 20),
                            LocalDate.of(2026, 10, 20), LocalDate.of(2027, 7, 25), 2340, "Ranchi"},
                    {"MeghaP", "9000000202", "megha202@gmail.com", "Silver", LocalDate.of(2026, 8, 17),
                            LocalDate.of(2026, 8, 17), LocalDate.of(2027, 8, 16), 704, "Patna"},
                    {"Sneha R", "9000000203", "sneha203@gmail.com", "Gold", LocalDate.of(2026, 9, 14),
                            LocalDate.of(2026, 9, 14), LocalDate.of(2027, 8, 2), 1291, "Bhopal"},
                    {"AnkitP", "9000000204", "ankit204@gmail.com", "Gold", LocalDate.of(2026, 10, 16),
                            LocalDate.of(2026, 10, 16), LocalDate.of(2027, 10, 7), 1072, "Nagpur"},
                    {"Rohan M", "9000000205", "rohan205@gmail.com", "Gold", LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2),
                            LocalDate.of(2027, 2, 3), 1325, "Pune"},
                    {"Sonia J", "9000000206", "sonia206@gmail.com", "Silver", LocalDate.of(2026, 7, 19),
                            LocalDate.of(2026, 7, 19), LocalDate.of(2027, 3, 24), 1725, "Jaipur"},
                    {"VivekT", "9000000207", "vivek207@gmail.com", "Platinum", LocalDate.of(2026, 2, 13),
                            LocalDate.of(2026, 2, 13), LocalDate.of(2027, 2, 11), 1081, "Lucknow"},
                    {"Riya S", "9000000208", "riya208@gmail.com", "Platinum", LocalDate.of(2026, 9, 23),
                            LocalDate.of(2026, 9, 23), LocalDate.of(2027, 4, 27), 1071, "Kolkata"},
                    {"PriyaR", "9000000209", "priya209@gmail.com", "Gold", LocalDate.of(2026, 8, 21),
                            LocalDate.of(2026, 8, 21), LocalDate.of(2027, 2, 24), 795, "Chennai"},
                    {"Rohan J", "9000000210", "rohan210@gmail.com", "Silver", LocalDate.of(2026, 1, 10),
                            LocalDate.of(2026, 1, 10), LocalDate.of(2026, 10, 7), 2212, "Delhi"},
                    {"VivekR", "9000000211", "vivek211@gmail.com", "Silver", LocalDate.of(2026, 6, 24),
                            LocalDate.of(2026, 6, 24), LocalDate.of(2026, 10, 28), 719, "Noida"},
                    {"Kiran V", "9000000212", "kiran212@gmail.com", "Gold", LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 7),
                            LocalDate.of(2026, 10, 31), 571, "Ranchi"},
                    {"NitinT", "9000000213", "nitin213@gmail.com", "Platinum", LocalDate.of(2026, 5, 2),
                            LocalDate.of(2026, 5, 2), LocalDate.of(2027, 4, 20), 1796, "Jaipur"},
                    {"KunalD", "9000000214", "kunal214@gmail.com", "Platinum", LocalDate.of(2026, 2, 11),
                            LocalDate.of(2026, 2, 11), LocalDate.of(2026, 10, 7), 762, "Nashik"},
                    {"Amit S", "9000000215", "amit215@gmail.com", "Gold", LocalDate.of(2026, 2, 7), LocalDate.of(2026, 2, 7),
                            LocalDate.of(2026, 6, 2), 1273, "Mumbai"},
                    {"Priya D", "9000000216", "priya216@gmail.com", "Gold", LocalDate.of(2026, 5, 8), LocalDate.of(2026, 5, 8),
                            LocalDate.of(2027, 2, 28), 1314, "Noida"},
                    {"Sonia R", "9000000217", "sonia217@gmail.com", "Gold", LocalDate.of(2026, 9, 25),
                            LocalDate.of(2026, 9, 25), LocalDate.of(2027, 4, 8), 1778, "Kolkata"},
                    {"MayankK", "9000000218", "mayank218@gmail.com", "Platinum", LocalDate.of(2026, 9, 27),
                            LocalDate.of(2026, 9, 27), LocalDate.of(2027, 9, 21), 2224, "Jaipur"},
                    {"Kiran S", "9000000219", "kiran219@gmail.com", "Silver", LocalDate.of(2026, 2, 12),
                            LocalDate.of(2026, 2, 12), LocalDate.of(2026, 6, 3), 1542, "Nagpur"},
                    {"AnkitM", "9000000220", "ankit220@gmail.com", "Silver", LocalDate.of(2026, 10, 18),
                            LocalDate.of(2026, 10, 18), LocalDate.of(2027, 7, 7), 1880, "Nagpur"},
                    {"PoojaS", "9000000221", "pooja221@gmail.com", "Platinum", LocalDate.of(2026, 12, 23),
                            LocalDate.of(2026, 12, 23), LocalDate.of(2027, 9, 17), 828, "Ranchi"},
                    {"Neha K", "9000000222", "neha222@gmail.com", "Gold", LocalDate.of(2026, 12, 20),
                            LocalDate.of(2026, 12, 20), LocalDate.of(2027, 4, 23), 2160, "Surat"},
                    {"Priya M", "9000000223", "priya223@gmail.com", "Gold", LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2),
                            LocalDate.of(2026, 10, 7), 2278, "Mumbai"},
                    {"Vikas V", "9000000224", "vikas224@gmail.com", "Gold", LocalDate.of(2026, 6, 10),
                            LocalDate.of(2026, 6, 10), LocalDate.of(2026, 10, 21), 588, "Jaipur"},
                    {"KunalN", "9000000225", "kunal225@gmail.com", "Gold", LocalDate.of(2026, 12, 29),
                            LocalDate.of(2026, 12, 29), LocalDate.of(2027, 8, 26), 2436, "Patna"},
                    {"Riya T", "9000000226", "riya226@gmail.com", "Gold", LocalDate.of(2026, 6, 19), LocalDate.of(2026, 6, 19),
                            LocalDate.of(2026, 10, 27), 1799, "Mumbai"},
                    {"DeepakJ", "9000000227", "deepak227@gmail.com", "Silver", LocalDate.of(2026, 10, 19),
                            LocalDate.of(2026, 10, 19), LocalDate.of(2027, 7, 20), 1541, "Delhi"},
                    {"Amit V", "9000000228", "amit228@gmail.com", "Silver", LocalDate.of(2026, 11, 27),
                            LocalDate.of(2026, 11, 27), LocalDate.of(2027, 9, 12), 1904, "Jaipur"},
                    {"RohanT", "9000000229", "rohan229@gmail.com", "Platinum", LocalDate.of(2026, 8, 22),
                            LocalDate.of(2026, 8, 22), LocalDate.of(2027, 1, 26), 2235, "Bhopal"},
                    {"Neha V", "9000000230", "neha230@gmail.com", "Platinum", LocalDate.of(2026, 10, 14),
                            LocalDate.of(2026, 10, 14), LocalDate.of(2027, 6, 1), 1154, "Bhopal"},
                    {"VikasN", "9000000231", "vikas231@gmail.com", "Silver", LocalDate.of(2026, 3, 17),
                            LocalDate.of(2026, 3, 17), LocalDate.of(2026, 12, 29), 898, "Bhopal"},
                    {"Ajay M", "9000000232", "ajay232@gmail.com", "Silver", LocalDate.of(2026, 10, 12),
                            LocalDate.of(2026, 10, 12), LocalDate.of(2027, 8, 17), 1228, "Ranchi"},
                    {"KunalP", "9000000233", "kunal233@gmail.com", "Silver", LocalDate.of(2026, 6, 4),
                            LocalDate.of(2026, 6, 4), LocalDate.of(2027, 1, 25), 2033, "Surat"},
                    {"PriyaM", "9000000234", "priya234@gmail.com", "Platinum", LocalDate.of(2026, 2, 26),
                            LocalDate.of(2026, 2, 26), LocalDate.of(2026, 11, 12), 1719, "Nagpur"},
                    {"Ajay P", "9000000235", "ajay235@gmail.com", "Platinum", LocalDate.of(2026, 8, 25),
                            LocalDate.of(2026, 8, 25), LocalDate.of(2027, 8, 24), 640, "Bhopal"},
                    {"AjayD", "9000000236", "ajay236@gmail.com", "Gold", LocalDate.of(2026, 1, 24), LocalDate.of(2026, 1, 24),
                            LocalDate.of(2026, 8, 15), 1067, "Nashik"},
                    {"MayankK", "9000000237", "mayank237@gmail.com", "Platinum", LocalDate.of(2026, 3, 9),
                            LocalDate.of(2026, 3, 9), LocalDate.of(2026, 12, 21), 1231, "Kolkata"},
                    {"KunalT", "9000000238", "kunal238@gmail.com", "Platinum", LocalDate.of(2026, 6, 11),
                            LocalDate.of(2026, 6, 11), LocalDate.of(2027, 5, 2), 1963, "Indore"},
                    {"Ankit S", "9000000239", "ankit239@gmail.com", "Gold", LocalDate.of(2026, 12, 8),
                            LocalDate.of(2026, 12, 8), LocalDate.of(2027, 3, 12), 1150, "Mumbai"},
                    {"SoniaT", "9000000240", "sonia240@gmail.com", "Platinum", LocalDate.of(2026, 3, 24),
                            LocalDate.of(2026, 3, 24), LocalDate.of(2027, 2, 9), 1011, "Lucknow"},
                    {"Pooja J", "9000000241", "pooja241@gmail.com", "Gold", LocalDate.of(2026, 5, 14),
                            LocalDate.of(2026, 5, 14), LocalDate.of(2027, 2, 9), 1860, "Bhopal"},
                    {"NitinN", "9000000242", "nitin242@gmail.com", "Platinum", LocalDate.of(2026, 10, 21),
                            LocalDate.of(2026, 10, 21), LocalDate.of(2027, 3, 24), 1457, "Patna"},
                    {"Vikas P", "9000000243", "vikas243@gmail.com", "Gold", LocalDate.of(2026, 5, 20),
                            LocalDate.of(2026, 5, 20), LocalDate.of(2026, 12, 18), 1163, "Chennai"},
                    {"AnkitD", "9000000244", "ankit244@gmail.com", "Platinum", LocalDate.of(2026, 7, 20),
                            LocalDate.of(2026, 7, 20), LocalDate.of(2027, 3, 21), 904, "Lucknow"},
                    {"RohanP", "9000000245", "rohan245@gmail.com", "Platinum", LocalDate.of(2026, 12, 13),
                            LocalDate.of(2026, 12, 13), LocalDate.of(2027, 6, 1), 2104, "Delhi"},
                    {"Priya N", "9000000246", "priya246@gmail.com", "Gold", LocalDate.of(2026, 10, 26),
                            LocalDate.of(2026, 10, 26), LocalDate.of(2027, 10, 2), 1625, "Noida"},
                    {"AmitP", "9000000247", "amit247@gmail.com", "Gold", LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 6),
                            LocalDate.of(2026, 12, 13), 1706, "Surat"},
                    {"SoniaK", "9000000248", "sonia248@gmail.com", "Platinum", LocalDate.of(2026, 3, 20),
                            LocalDate.of(2026, 3, 20), LocalDate.of(2027, 2, 15), 1008, "Noida"},
                    {"Vivek V", "9000000249", "vivek249@gmail.com", "Silver", LocalDate.of(2026, 10, 26),
                            LocalDate.of(2026, 10, 26), LocalDate.of(2027, 5, 23), 1066, "Kolkata"},
                    {"SnehaT", "9000000250", "sneha250@gmail.com", "Gold", LocalDate.of(2026, 6, 17),
                            LocalDate.of(2026, 6, 17), LocalDate.of(2026, 9, 23), 1788, "Kolkata"},
                    {"Kunal J", "9000000251", "kunal251@gmail.com", "Gold", LocalDate.of(2026, 1, 10),
                            LocalDate.of(2026, 1, 10), LocalDate.of(2026, 7, 25), 1238, "Nashik"},
                    {"AmitR", "9000000252", "amit252@gmail.com", "Platinum", LocalDate.of(2026, 2, 9),
                            LocalDate.of(2026, 2, 9), LocalDate.of(2026, 5, 29), 1386, "Noida"},
                    {"Riya T", "9000000253", "riya253@gmail.com", "Platinum", LocalDate.of(2026, 4, 17),
                            LocalDate.of(2026, 4, 17), LocalDate.of(2026, 11, 23), 842, "Kolkata"},
                    {"AmitT", "9000000254", "amit254@gmail.com", "Platinum", LocalDate.of(2026, 5, 20),
                            LocalDate.of(2026, 5, 20), LocalDate.of(2027, 3, 31), 2051, "Kolkata"},
                    {"Nitin J", "9000000255", "nitin255@gmail.com", "Silver", LocalDate.of(2026, 4, 19),
                            LocalDate.of(2026, 4, 19), LocalDate.of(2027, 1, 23), 892, "Jaipur"},
                    {"MayankK", "9000000256", "mayank256@gmail.com", "Gold", LocalDate.of(2026, 8, 9),
                            LocalDate.of(2026, 8, 9), LocalDate.of(2027, 1, 1), 2017, "Bhopal"},
                    {"Ajay K", "9000000257", "ajay257@gmail.com", "Gold", LocalDate.of(2026, 12, 27),
                            LocalDate.of(2026, 12, 27), LocalDate.of(2027, 5, 1), 1785, "Indore"},
                    {"SoniaM", "9000000258", "sonia258@gmail.com", "Platinum", LocalDate.of(2026, 7, 15),
                            LocalDate.of(2026, 7, 15), LocalDate.of(2027, 3, 23), 693, "Surat"},
                    {"Mayank D", "9000000259", "mayank259@gmail.com", "Gold", LocalDate.of(2026, 8, 24),
                            LocalDate.of(2026, 8, 24), LocalDate.of(2027, 6, 15), 2224, "Nashik"},
                    {"MeghaJ", "9000000260", "megha260@gmail.com", "Gold", LocalDate.of(2026, 5, 28),
                            LocalDate.of(2026, 5, 28), LocalDate.of(2027, 2, 1), 988, "Chennai"},
                    {"Pooja D", "9000000261", "pooja261@gmail.com", "Silver", LocalDate.of(2026, 7, 8),
                            LocalDate.of(2026, 7, 8), LocalDate.of(2027, 3, 5), 1641, "Patna"},
                    {"VikasT", "9000000262", "vikas262@gmail.com", "Platinum", LocalDate.of(2026, 12, 5),
                            LocalDate.of(2026, 12, 5), LocalDate.of(2027, 3, 31), 1641, "Mumbai"},
                    {"RohanD", "9000000263", "rohan263@gmail.com", "Platinum", LocalDate.of(2026, 2, 8),
                            LocalDate.of(2026, 2, 8), LocalDate.of(2026, 12, 24), 2393, "Lucknow"},
                    {"Pooja V", "9000000264", "pooja264@gmail.com", "Silver", LocalDate.of(2026, 5, 16),
                            LocalDate.of(2026, 5, 16), LocalDate.of(2027, 1, 21), 1768, "Jaipur"},
                    {"ArjunT", "9000000265", "arjun265@gmail.com", "Gold", LocalDate.of(2026, 3, 16),
                            LocalDate.of(2026, 3, 16), LocalDate.of(2027, 1, 29), 2107, "Nashik"},
                    {"Vikas R", "9000000266", "vikas266@gmail.com", "Gold", LocalDate.of(2026, 4, 13),
                            LocalDate.of(2026, 4, 13), LocalDate.of(2026, 10, 20), 1008, "Noida"},
                    {"PriyaT", "9000000267", "priya267@gmail.com", "Gold", LocalDate.of(2026, 11, 7),
                            LocalDate.of(2026, 11, 7), LocalDate.of(2027, 8, 19), 2394, "Bhopal"},
                    {"Riya J", "9000000268", "riya268@gmail.com", "Gold", LocalDate.of(2026, 6, 12), LocalDate.of(2026, 6, 12),
                            LocalDate.of(2027, 1, 3), 678, "Pune"},
                    {"Riya M", "9000000269", "riya269@gmail.com", "Silver", LocalDate.of(2026, 4, 28),
                            LocalDate.of(2026, 4, 28), LocalDate.of(2026, 12, 30), 814, "Noida"},
                    {"ArjunT", "9000000270", "arjun270@gmail.com", "Gold", LocalDate.of(2026, 10, 2),
                            LocalDate.of(2026, 10, 2), LocalDate.of(2027, 9, 13), 755, "Kolkata"},
                    {"Megha J", "9000000271", "megha271@gmail.com", "Gold", LocalDate.of(2026, 8, 15),
                            LocalDate.of(2026, 8, 15), LocalDate.of(2027, 2, 19), 1930, "Bhopal"},
                    {"RahulV", "9000000272", "rahul272@gmail.com", "Gold", LocalDate.of(2026, 6, 20),
                            LocalDate.of(2026, 6, 20), LocalDate.of(2026, 11, 7), 1362, "Pune"},
                    {"Sneha P", "9000000273", "sneha273@gmail.com", "Gold", LocalDate.of(2026, 9, 26),
                            LocalDate.of(2026, 9, 26), LocalDate.of(2027, 2, 8), 2132, "Patna"},
                    {"Vivek P", "9000000274", "vivek274@gmail.com", "Gold", LocalDate.of(2026, 9, 22),
                            LocalDate.of(2026, 9, 22), LocalDate.of(2027, 1, 23), 1667, "Patna"},
                    {"ArjunJ", "9000000275", "arjun275@gmail.com", "Silver", LocalDate.of(2026, 3, 6),
                            LocalDate.of(2026, 3, 6), LocalDate.of(2026, 6, 4), 1224, "Nashik"},
                    {"MayankT", "9000000276", "mayank276@gmail.com", "Silver", LocalDate.of(2026, 5, 26),
                            LocalDate.of(2026, 5, 26), LocalDate.of(2027, 5, 24), 2449, "Bhopal"},
                    {"Sonia K", "9000000277", "sonia277@gmail.com", "Silver", LocalDate.of(2026, 11, 14),
                            LocalDate.of(2026, 11, 14), LocalDate.of(2027, 7, 28), 1559, "Nashik"},
                    {"RiyaP", "9000000278", "riya278@gmail.com", "Silver", LocalDate.of(2026, 2, 15),
                            LocalDate.of(2026, 2, 15), LocalDate.of(2026, 12, 27), 869, "Pune"},
                    {"Rohan S", "9000000279", "rohan279@gmail.com", "Silver", LocalDate.of(2026, 3, 30),
                            LocalDate.of(2026, 3, 30), LocalDate.of(2026, 9, 17), 1971, "Lucknow"},
                    {"SnehaS", "9000000280", "sneha280@gmail.com", "Platinum", LocalDate.of(2026, 5, 20),
                            LocalDate.of(2026, 5, 20), LocalDate.of(2027, 1, 1), 547, "Chennai"},
                    {"ArjunJ", "9000000281", "arjun281@gmail.com", "Platinum", LocalDate.of(2026, 4, 30),
                            LocalDate.of(2026, 4, 30), LocalDate.of(2026, 9, 9), 1823, "Nashik"},
                    {"DeepakM", "9000000282", "deepak282@gmail.com", "Platinum", LocalDate.of(2026, 6, 2),
                            LocalDate.of(2026, 6, 2), LocalDate.of(2027, 3, 17), 1619, "Lucknow"},
                    {"Vikas S", "9000000283", "vikas283@gmail.com", "Silver", LocalDate.of(2026, 4, 6),
                            LocalDate.of(2026, 4, 6), LocalDate.of(2026, 11, 7), 2208, "Surat"},
                    {"Vivek J", "9000000284", "vivek284@gmail.com", "Gold", LocalDate.of(2026, 4, 19),
                            LocalDate.of(2026, 4, 19), LocalDate.of(2027, 3, 27), 931, "Delhi"},
                    {"Sonia P", "9000000285", "sonia285@gmail.com", "Silver", LocalDate.of(2026, 7, 25),
                            LocalDate.of(2026, 7, 25), LocalDate.of(2027, 4, 1), 1260, "Chennai"},
                    {"SoniaR", "9000000286", "sonia286@gmail.com", "Gold", LocalDate.of(2026, 6, 23),
                            LocalDate.of(2026, 6, 23), LocalDate.of(2027, 1, 12), 1318, "Lucknow"},
                    {"Riya P", "9000000287", "riya287@gmail.com", "Gold", LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 12),
                            LocalDate.of(2027, 5, 11), 1851, "Mumbai"},
                    {"Ajay J", "9000000288", "ajay288@gmail.com", "Gold", LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 2),
                            LocalDate.of(2027, 9, 1), 2110, "Mumbai"},
                    {"VivekS", "9000000289", "vivek289@gmail.com", "Platinum", LocalDate.of(2026, 3, 9),
                            LocalDate.of(2026, 3, 9), LocalDate.of(2026, 10, 19), 607, "Delhi"},
                    {"Rohan T", "9000000290", "rohan290@gmail.com", "Gold", LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 8),
                            LocalDate.of(2026, 11, 29), 2176, "Indore"},
                    {"VivekP", "9000000291", "vivek291@gmail.com", "Gold", LocalDate.of(2026, 11, 30),
                            LocalDate.of(2026, 11, 30), LocalDate.of(2027, 11, 15), 600, "Noida"},
                    {"VivekJ", "9000000292", "vivek292@gmail.com", "Platinum", LocalDate.of(2026, 6, 29),
                            LocalDate.of(2026, 6, 29), LocalDate.of(2027, 2, 27), 1287, "Delhi"},
                    {"Vikas R", "9000000293", "vikas293@gmail.com", "Gold", LocalDate.of(2026, 5, 9), LocalDate.of(2026, 5, 9),
                            LocalDate.of(2026, 11, 23), 879, "Delhi"},
                    {"Sonia K", "9000000294", "sonia294@gmail.com", "Silver", LocalDate.of(2026, 9, 27),
                            LocalDate.of(2026, 9, 27), LocalDate.of(2027, 1, 5), 711, "Indore"},
                    {"Sneha R", "9000000295", "sneha295@gmail.com", "Gold", LocalDate.of(2026, 6, 29),
                            LocalDate.of(2026, 6, 29), LocalDate.of(2026, 10, 22), 1266, "Indore"},
                    {"VikasR", "9000000296", "vikas296@gmail.com", "Platinum", LocalDate.of(2026, 8, 6),
                            LocalDate.of(2026, 8, 6), LocalDate.of(2027, 7, 30), 1255, "Lucknow"},
                    {"Ankit M", "9000000297", "ankit297@gmail.com", "Silver", LocalDate.of(2026, 5, 11),
                            LocalDate.of(2026, 5, 11), LocalDate.of(2026, 9, 1), 1426, "Noida"},
                    {"Ajay R", "9000000298", "ajay298@gmail.com", "Gold", LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 28),
                            LocalDate.of(2026, 9, 27), 819, "Jaipur"},
                    {"KunalS", "9000000299", "kunal299@gmail.com", "Platinum", LocalDate.of(2026, 2, 26),
                            LocalDate.of(2026, 2, 26), LocalDate.of(2026, 7, 25), 1658, "Noida"},
                    {"Suhan D", "9000000300", "rohan300@gmail.com", "Platinum", LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 4), LocalDate.of(2026, 12, 22), 1259, "Surat"},
                    {"Rohan N", "9000000301", "rohan301@gmail.com", "Platinum", LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 14), LocalDate.of(2027, 6, 22), 2207, "Surat"},
                    {"Kiran K", "9000000302", "kiran302@gmail.com", "Gold", LocalDate.of(2026, 7, 9), LocalDate.of(2026, 7, 9), LocalDate.of(2027, 5, 2), 1370, "Chennai"},
                    {"Vikas P", "9000000303", "vikas303@gmail.com", "Gold", LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 18), LocalDate.of(2026, 11, 27), 894, "Nagpur"},
                    {"Kiran S", "9000000304", "kiran304@gmail.com", "Silver", LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 4), LocalDate.of(2026, 11, 19), 2020, "Noida"},
                    {"Kunal P", "9000000305", "kunal305@gmail.com", "Platinum", LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 21), LocalDate.of(2027, 2, 28), 1789, "Bhopal"},
                    {"Mayank K", "9000000306", "mayank306@gmail.com", "Platinum", LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 27), LocalDate.of(2027, 8, 31), 1408, "Surat"},
                    {"Sneha K", "9000000307", "sneha307@gmail.com", "Silver", LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31), LocalDate.of(2027, 4, 29), 1069, "Bhopal"},
                    {"Pooja M", "9000000308", "pooja308@gmail.com", "Gold", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2027, 10, 1), 1436, "Nashik"},
                    {"Ajay V", "9000000309", "ajay309@gmail.com", "Platinum", LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 20), LocalDate.of(2027, 3, 4), 2352, "Surat"},
                    {"Vikas M", "9000000310", "vikas310@gmail.com", "Silver", LocalDate.of(2026, 5, 8), LocalDate.of(2026, 5, 8), LocalDate.of(2027, 3, 18), 1670, "Mumbai"},
                    {"Kiran T", "9000000311", "kiran311@gmail.com", "Gold", LocalDate.of(2026, 4, 25), LocalDate.of(2026, 4, 25), LocalDate.of(2027, 3, 30), 1101, "Lucknow"},
                    {"Vikas V", "9000000312", "vikas312@gmail.com", "Silver", LocalDate.of(2026, 4, 28), LocalDate.of(2026, 4, 28), LocalDate.of(2027, 3, 1), 1208, "Patna"},
                    {"Sonia V", "9000000313", "sonia313@gmail.com", "Platinum", LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 3), LocalDate.of(2027, 10, 13), 1778, "Patna"},
                    {"Amit P", "9000000314", "amit314@gmail.com", "Platinum", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 15), 1976, "Mumbai"},
                    {"Rohan J", "9000000315", "rohan315@gmail.com", "Silver", LocalDate.of(2026, 12, 16), LocalDate.of(2026, 12, 16), LocalDate.of(2027, 9, 13), 2146, "Delhi"},
                    {"Neha V", "9000000316", "neha316@gmail.com", "Silver", LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21), LocalDate.of(2027, 2, 26), 1088, "Noida"},
                    {"Sonia T", "9000000317", "sonia317@gmail.com", "Platinum", LocalDate.of(2026, 1, 22), LocalDate.of(2026, 1, 22), LocalDate.of(2026, 12, 20), 1132, "Patna"},
                    {"Sonia M", "9000000318", "sonia318@gmail.com", "Silver", LocalDate.of(2026, 8, 26), LocalDate.of(2026, 8, 26), LocalDate.of(2027, 4, 3), 1492, "Ranchi"},
                    {"Sneha T", "9000000319", "sneha319@gmail.com", "Silver", LocalDate.of(2026, 7, 28), LocalDate.of(2026, 7, 28), LocalDate.of(2026, 11, 12), 950, "Indore"},
                    {"Rohan T", "9000000320", "rohan320@gmail.com", "Silver", LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 8, 26), 2195, "Pune"},
                    {"Megha K", "9000000321", "megha321@gmail.com", "Platinum", LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 6), LocalDate.of(2027, 4, 8), 1066, "Noida"},
                    {"Vikas S", "9000000322", "vikas322@gmail.com", "Silver", LocalDate.of(2026, 3, 18), LocalDate.of(2026, 3, 18), LocalDate.of(2026, 12, 9), 677, "Lucknow"},
                    {"Vikas N", "9000000323", "vikas323@gmail.com", "Silver", LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 24), LocalDate.of(2027, 7, 6), 1465, "Ranchi"},
                    {"Rohan R", "9000000324", "rohan324@gmail.com", "Silver", LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 26), LocalDate.of(2027, 8, 11), 2419, "Ranchi"},
                    {"Rahul N", "9000000325", "rahul325@gmail.com", "Silver", LocalDate.of(2026, 2, 22), LocalDate.of(2026, 2, 22), LocalDate.of(2026, 6, 4), 2253, "Indore"},
                    {"Riya V", "9000000326", "riya326@gmail.com", "Platinum", LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 6), LocalDate.of(2027, 1, 15), 2359, "Ranchi"},
                    {"Amit P", "9000000327", "amit327@gmail.com", "Silver", LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 18), LocalDate.of(2026, 11, 5), 1945, "Kolkata"},
                    {"Rahul M", "9000000328", "rahul328@gmail.com", "Silver", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), LocalDate.of(2027, 6, 3), 639, "Delhi"},
                    {"Ankit J", "9000000329", "ankit329@gmail.com", "Silver", LocalDate.of(2026, 2, 6), LocalDate.of(2026, 2, 6), LocalDate.of(2026, 8, 23), 805, "Nashik"}
            };

            int rowNum = 13;

            for (Object[] values : sampleData) {

                Row row = sheet.createRow(rowNum++);

                for (int col = 0; col < values.length; col++) {

                    Cell cell = row.createCell(col);

                    Object value = values[col];

                    if (value == null) {

                        cell.setCellValue("");
                        cell.setCellStyle(normalStyle);
                        continue;

                    }

                    if (value instanceof String text) {

                        cell.setCellValue(text);
                        cell.setCellStyle(normalStyle);

                    } else if (value instanceof Integer number) {

                        cell.setCellValue(number);
                        cell.setCellStyle(amountStyle);

                    } else if (value instanceof Long number) {

                        cell.setCellValue(number);
                        cell.setCellStyle(amountStyle);

                    } else if (value instanceof Double number) {

                        cell.setCellValue(number);
                        cell.setCellStyle(amountStyle);

                    } else if (value instanceof LocalDate date) {

                        cell.setCellValue(
                                Date.valueOf(date)
                        );

                        cell.setCellStyle(dateStyle);

                    } else {

                        cell.setCellValue(value.toString());
                        cell.setCellStyle(normalStyle);

                    }

                }

            }

            // =========================================================
            // FREEZE HEADER
            // =========================================================

            sheet.createFreezePane(
                    0,
                    13
            );

            // =========================================================
            // AUTO FILTER
            // =========================================================

            sheet.setAutoFilter(

                    new CellRangeAddress(

                            12,
                            12,
                            0,
                            headers.length - 1

                    )

            );

            // =========================================================
            // COLUMN WIDTHS
            // =========================================================

            sheet.setColumnWidth(0, 7000); // Name
            sheet.setColumnWidth(1, 5000); // Phone
            sheet.setColumnWidth(2, 9000); // Email
            sheet.setColumnWidth(3, 5000); // Plan
            sheet.setColumnWidth(4, 4500); // Joining Date
            sheet.setColumnWidth(5, 4500); // Start Date
            sheet.setColumnWidth(6, 4500); // Expiry Date
            sheet.setColumnWidth(7, 4500); // Due Amount
            sheet.setColumnWidth(8, 9000); // Address

            //Default data types
            // Text columns
            sheet.setDefaultColumnStyle(0, textStyle); // Name
            sheet.setDefaultColumnStyle(1, textStyle); // Phone
            sheet.setDefaultColumnStyle(2, textStyle); // Email
            sheet.setDefaultColumnStyle(3, textStyle); // Plan
            sheet.setDefaultColumnStyle(8, textStyle); // Address

            // Date columns
            sheet.setDefaultColumnStyle(4, dateStyle); // Joining Date
            sheet.setDefaultColumnStyle(5, dateStyle); // Start Date
            sheet.setDefaultColumnStyle(6, dateStyle); // Expiry Date

            // Number column
            sheet.setDefaultColumnStyle(7, amountStyle); // Due Amount

            // =========================================================
            // DATA VALIDATION (Plan)
            // =========================================================

            DataValidationHelper validationHelper =
                    sheet.getDataValidationHelper();

            DataValidationConstraint planConstraint =
                    validationHelper.createExplicitListConstraint(

                            new String[]{

                                    "Gold",
                                    "Silver",
                                    "Premium"

                            }

                    );

            CellRangeAddressList addressList =
                    new CellRangeAddressList(

                            13,
                            5000,
                            3,
                            3

                    );

            DataValidation validation =
                    validationHelper.createValidation(

                            planConstraint,
                            addressList

                    );

            validation.setSuppressDropDownArrow(false);

            sheet.addValidationData(validation);

            // =========================================================
            // WRITE
            // =========================================================

            workbook.write(out);
            workbook.close();
            out.flush();

            return out.toByteArray();

        } catch (Exception e) {
            System.out.println(e);

            throw new RuntimeException(

                    "Failed to generate member import template.",

                    e

            );

        }

    }

//    public byte[] downloadMemberImportTemplates() {
//
//        try (
//                Workbook workbook = new XSSFWorkbook();
//                ByteArrayOutputStream out = new ByteArrayOutputStream()
//        ) {
//
//            Sheet sheet = workbook.createSheet("Members Import Template");
//            sheet.setDefaultColumnStyle(0, null); // no-op placeholder, keeps structure explicit
//
//            // =========================
//            // TITLE STYLE
//            // =========================
//
//            CellStyle titleStyle = workbook.createCellStyle();
//
//            Font titleFont = workbook.createFont();
//            titleFont.setBold(true);
//            titleFont.setFontHeightInPoints((short) 16);
//
//            titleStyle.setFont(titleFont);
//            titleStyle.setAlignment(HorizontalAlignment.CENTER);
//
//            // =========================
//            // INSTRUCTION STYLES
//            // =========================
//
//            CellStyle instructionHeaderStyle = workbook.createCellStyle();
//            Font instructionHeaderFont = workbook.createFont();
//            instructionHeaderFont.setBold(true);
//            instructionHeaderStyle.setFont(instructionHeaderFont);
//
//            CellStyle instructionStyle = workbook.createCellStyle();
//            Font instructionFont = workbook.createFont();
//            instructionFont.setBold(false);
//            instructionStyle.setFont(instructionFont);
//
//            // =========================
//            // HEADER STYLE (light green fill, no border)
//            // =========================
//
//            CellStyle headerStyle = workbook.createCellStyle();
//
//            Font headerFont = workbook.createFont();
//            headerFont.setBold(true);
//            headerStyle.setFont(headerFont);
//
//            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{
//                    (byte) 0xD9, (byte) 0xEA, (byte) 0xD3
//            }, null));
//            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//            // =========================
//            // DATE STYLE
//            // =========================
//
//            CellStyle dateStyle = workbook.createCellStyle();
//            CreationHelper createHelper = workbook.getCreationHelper();
//            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm-dd-yy"));
//
//            // =========================
//            // TITLE (row 1, merged A1:I1)
//            // =========================
//
//            Row titleRow = sheet.createRow(0);
//            titleRow.setHeightInPoints(21);
//
//            Cell titleCell = titleRow.createCell(0);
//            titleCell.setCellValue("Gym SaaS - Members Import Template");
//            titleCell.setCellStyle(titleStyle);
//
//            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
//
//            // =========================
//            // INSTRUCTIONS (rows 2-10)
//            // =========================
//
//            Font normalFont = workbook.createFont();
//            normalFont.setFontHeightInPoints((short) 11);
//
//            Font boldRedFont = workbook.createFont();
//            boldRedFont.setBold(true);
//            boldRedFont.setColor(IndexedColors.RED.getIndex());
//            boldRedFont.setFontHeightInPoints((short) 11);
//
//            Font boldFont = workbook.createFont();
//            boldFont.setBold(true);
//            boldFont.setFontHeightInPoints((short) 11);
//
//            String[] instructions = {
//                    "Instructions - Please read instructions carefully before importing Member data.",
//                    "Do not modify header names.",
//                    "One member per row.",
//                    "Dates: DD-MM-YYYY (e.g. 12-07-2026).",
//                    "Address: Keep Address in max one or two words only. ",
//                    "Phone: 10 digits.",
//                    "Due Amount: Whole number.",
//                    "Plan: Make sure the plan name should be present in your 'Plans' section",
//                    "Required Fields - Name, Plan, Joining Date, Start Date, Expiry Date, and Due Amount."
//            };
//
//            String[] highlights = {
//
//                    "Please read",
//                    "modify",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "",
//                    "Name, Plan, Joining Date, Start Date, Expiry Date, and Due Amount"
//
//            };
//
//            for (int i = 0; i < instructions.length; i++) {
//
//                Row row = sheet.createRow(1 + i);
//
//                Cell cell = row.createCell(0);
//
//                if (highlights[i].isBlank()) {
//
//                    cell.setCellValue(instructions[i]);
//
//                } else {
//
//                    cell.setCellValue(
//
//                            createInstruction(
//
//                                    instructions[i],
//
//                                    highlights[i],
//
//                                    normalFont,
//
//                                    boldRedFont
//
//                            )
//
//                    );
//
//                }
//
//                cell.setCellStyle(
//
//                        i == 0
//
//                                ? instructionHeaderStyle
//
//                                : instructionStyle
//
//                );
//
//            }
//
//            // Row 10 (index 10) is left blank as a spacer before headers
//
//            // =========================
//            // HEADERS (row 12 -> index 11)
//            // =========================
//
//            String[] headers = {
//                    "Name", "Phone", "Email", "Plan", "Joining Date",
//                    "Start Date", "Expiry Date", "Due Amount", "Address"
//            };
//
//            Row headerRow = sheet.createRow(11);
//            headerRow.setHeightInPoints(15);
//
//            for (int i = 0; i < headers.length; i++) {
//                Cell cell = headerRow.createCell(i);
//                cell.setCellValue(headers[i]);
//                cell.setCellStyle(headerStyle);
//            }
//
//            // =========================
//            // SAMPLE DATA (rows 13-15 -> index 12-14)
//            // =========================
//
//            Object[][] sample = {
//                    {"Raj Chauhan", "9876543210", "raj@example.com", "Gold",
//                            LocalDate.of(2026, 7, 12), LocalDate.of(2026, 12, 6), LocalDate.of(2026, 7, 30), 0, "New Panvel"},
//
//                    {"Priya Sharma", "9123456780", "priya@example.com", "Silver",
//                            LocalDate.of(2026, 12, 6), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 9, 15), 500, "karanjade"},
//
//                    {"Amit Patel", "9988776655", "amit@example.com", "Platinum",
//                            LocalDate.of(2026, 6, 12), LocalDate.of(2026, 5, 2), LocalDate.of(2027, 3, 1), 0, "Navi Mumbai"}
//            };
//
//            int rowNum = 12;
//
//            for (Object[] data : sample) {
//                Row row = sheet.createRow(rowNum++);
//                row.setHeightInPoints(15);
//
//                for (int i = 0; i < data.length; i++) {
//                    Cell cell = row.createCell(i);
//                    Object value = data[i];
//
//                    if (value instanceof LocalDate localDate) {
//                        cell.setCellValue(java.sql.Date.valueOf(localDate));
//                        cell.setCellStyle(dateStyle);
//                    } else if (value instanceof Integer intValue) {
//                        cell.setCellValue(intValue);
//                    } else {
//                        cell.setCellValue(String.valueOf(value));
//                    }
//                }
//            }
//
//            // =========================
//            // COLUMN WIDTHS
//            // =========================
//
//            sheet.setColumnWidth(0, 25 * 256); // Name
//            sheet.setColumnWidth(1, 18 * 256); // Phone
//            sheet.setColumnWidth(2, 30 * 256); // Email
//            sheet.setColumnWidth(3, 18 * 256); // Plan
//            sheet.setColumnWidth(4, 16 * 256); // Joining Date
//            sheet.setColumnWidth(5, 13 * 256); // Start Date
//            sheet.setColumnWidth(6, 13 * 256); // Expiry Date
//            sheet.setColumnWidth(7, 14 * 256); // Due Amount
//            sheet.setColumnWidth(8, 12 * 256); // Address
//
//            workbook.write(out);
//
//            return out.toByteArray();
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to generate template.", e);
//        }
//    }

    private XSSFRichTextString createInstruction(
            String text,
            String highlight,
            Font normalFont,
            Font highlightFont) {

        XSSFRichTextString richText = new XSSFRichTextString(text);

        richText.applyFont(normalFont);

        int start = text.indexOf(highlight);

        if (start >= 0) {

            richText.applyFont(
                    start,
                    start + highlight.length(),
                    highlightFont
            );

        }

        return richText;
    }

    private Member buildMember(MemberImportDto dto, Owner owner, MemberShip plan) {
        Member member = new Member();
        member.setName(dto.getName() != null ? dto.getName() : member.getName());
        member.setPhone(dto.getPhone() != null ? dto.getPhone() : member.getPhone());
        member.setEmail(dto.getEmail() != null ? dto.getEmail() : member.getEmail());
        member.setAddress(dto.getAddress() != null ? dto.getAddress() : member.getAddress());
        member.setJoined(dto.getJoiningDate() != null ? dto.getJoiningDate() : member.getJoined());
        member.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : member.getStartDate());
        member.setExpiry(dto.getExpiryDate() != null ? dto.getExpiryDate() : member.getExpiry());
        member.setDueAmount(dto.getDueAmount() != null ? dto.getDueAmount() : member.getDueAmount());
        member.setMemberShip(plan != null ? plan : member.getMemberShip());
        member.setOwner(owner != null ? owner : member.getOwner());
        member.setIsActive(1); // Constant value never needs a null check
        return member;
    }

}

