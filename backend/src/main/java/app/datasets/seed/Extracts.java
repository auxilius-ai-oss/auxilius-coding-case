package app.datasets.seed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The quarterly ERP extracts the seed and the sample uploads are made of. Everything is generated from a fixed
 * random seed, so every run produces the same files.
 */
public final class Extracts {

    /** How a column's values are generated. ID and DATE_DE are the ones inference gets wrong. */
    public enum Kind {
        CODE, DOCNO, YEAR, DATE_DE, DATE, TS, USER, DOCTYPE, TEXT, ID, AMOUNT, CUR, DC, INT, TAX, UNIT, COMPANY,
        COUNTRY, IBAN, TERMS, FLAG, BOOL, PERSON, ROLE, EMAIL, STATUS
    }

    public record Column(String header, Kind kind) {}

    /** {@code fileName} may contain {period}, replaced with e.g. 2026-12. */
    public record Extract(String fileName, char separator, int rows, Quarter since, List<Column> columns) {

        public String fileNameFor(Quarter q) {
            return fileName.replace("{period}", q.period());
        }

        public boolean existsIn(Quarter q) {
            return !q.isBefore(since);
        }

        public Extract withHeader(int index, String header) {
            List<Column> copy = new ArrayList<>(columns);
            copy.set(index, new Column(header, copy.get(index).kind()));
            return new Extract(fileName, separator, rows, since, copy);
        }
    }

    public static final String COST_CENTRE_WORKBOOK = "Cost_centre_hierarchy_FY{year}.xlsx";

    private static final Quarter FIRST = new Quarter(2023, 1);

    public static final List<Extract> ALL = List.of(
            csv("BKPF_journal_entry_headers.csv", ',', 1800, FIRST,
                    "company_code:CODE", "document_number:DOCNO", "fiscal_year:YEAR", "document_type:DOCTYPE",
                    "posting_date:DATE_DE", "document_date:DATE_DE", "entered_by:USER", "reference:TEXT",
                    "header_text:TEXT", "entered_at:TS"),
            csv("BSEG_journal_entry_line_items.csv", ',', 3000, FIRST,
                    "company_code:CODE", "document_number:DOCNO", "line_item:INT", "gl_account:ID", "cost_center:ID",
                    "profit_center:ID", "vendor_id:ID", "customer_id:ID", "amount_local:AMOUNT", "currency:CUR",
                    "debit_credit:DC", "posting_key:INT", "tax_code:TAX", "assignment:TEXT", "clearing_date:DATE_DE",
                    "quantity:INT", "unit:UNIT", "line_text:TEXT", "created_at:TS"),
            csv("Vendor_master.csv", ',', 900, FIRST,
                    "vendor_id:ID", "vendor_name:COMPANY", "country:COUNTRY", "iban:IBAN", "payment_terms:TERMS",
                    "posting_block:FLAG", "created_on:DATE_DE", "created_by:USER"),
            csv("Customer_master.csv", ',', 1100, FIRST,
                    "customer_id:ID", "customer_name:COMPANY", "country:COUNTRY", "region:CODE", "payment_terms:TERMS",
                    "credit_limit:AMOUNT", "created_on:DATE_DE", "blocked:FLAG"),
            csv("AP_open_items.csv", ',', 1400, FIRST,
                    "company_code:CODE", "vendor_id:ID", "document_number:DOCNO", "invoice_date:DATE_DE",
                    "due_date:DATE_DE", "amount:AMOUNT", "currency:CUR", "status:STATUS"),
            csv("AP_invoice_lines.csv", ',', 3000, FIRST,
                    "document_number:DOCNO", "line_item:INT", "vendor_id:ID", "gl_account:ID", "amount:AMOUNT",
                    "currency:CUR", "tax_code:TAX", "description:TEXT", "approved_by:USER", "approved_at:TS"),
            csv("AP_payment_runs.csv", ';', 700, FIRST,
                    "run_date:DATE_DE", "run_id:TEXT", "vendor_id:ID", "document_number:DOCNO", "paid_amount:AMOUNT",
                    "currency:CUR", "iban:IBAN", "released_by:USER"),
            csv("AR_open_items.csv", ',', 1200, FIRST,
                    "company_code:CODE", "customer_id:ID", "document_number:DOCNO", "invoice_date:DATE_DE",
                    "due_date:DATE_DE", "amount:AMOUNT", "currency:CUR", "dunning_level:INT"),
            csv("GL_account_master.csv", ',', 450, FIRST,
                    "gl_account:ID", "account_name:TEXT", "account_group:CODE", "balance_sheet_account:BOOL",
                    "blocked_for_posting:FLAG", "created_on:DATE_DE"),
            csv("USR02_user_master.csv", '\t', 600, FIRST,
                    "user_name:USER", "user_type:CODE", "valid_from:DATE_DE", "valid_to:DATE_DE",
                    "last_logon:TS", "locked:FLAG", "email:EMAIL"),
            csv("AGR_USERS_role_assignments.csv", '\t', 2200, FIRST,
                    "user_name:USER", "role:ROLE", "valid_from:DATE_DE", "valid_to:DATE_DE", "assigned_by:USER"),
            csv("CDHDR_change_documents.csv", ',', 2500, FIRST,
                    "object_class:CODE", "object_id:ID", "change_number:DOCNO", "changed_by:USER",
                    "changed_at:TS", "transaction_code:CODE"),
            csv("EKKO_purchase_orders.csv", ',', 1300, FIRST,
                    "purchase_order:DOCNO", "company_code:CODE", "vendor_id:ID", "order_date:DATE_DE",
                    "created_by:USER", "currency:CUR", "net_value:AMOUNT"),
            csv("EKPO_purchase_order_items.csv", ',', 2600, FIRST,
                    "purchase_order:DOCNO", "item:INT", "material:ID", "quantity:INT", "unit:UNIT",
                    "net_price:AMOUNT", "plant:CODE"),
            csv("MSEG_goods_receipts.csv", ',', 2400, FIRST,
                    "material_document:DOCNO", "purchase_order:DOCNO", "material:ID", "quantity:INT", "unit:UNIT",
                    "posting_date:DATE_DE", "posted_by:USER"),
            csv("Fixed_asset_register.csv", ';', 800, FIRST,
                    "asset_id:ID", "Asset description:TEXT", "company_code:CODE", "Acquisition value (EUR):AMOUNT",
                    "1st depreciation date:DATE_DE", "useful_life_years:INT", "cost_center:ID"),
            csv("Depreciation_run.csv", ';', 800, FIRST,
                    "asset_id:ID", "period:INT", "depreciation_amount:AMOUNT", "posted_on:DATE_DE", "run_by:USER"),
            csv("Payroll_summary_by_cost_centre_{period}.csv", ',', 300, new Quarter(2024, 1),
                    "cost_center:ID", "headcount:INT", "gross_pay:AMOUNT", "employer_costs:AMOUNT",
                    "currency:CUR", "approved_by:USER"),
            csv("Bank_accounts_master.csv", ',', 60, FIRST,
                    "house_bank:CODE", "account_id:CODE", "iban:IBAN", "currency:CUR", "gl_account:ID",
                    "signatory_1:PERSON", "signatory_2:PERSON"),
            csv("Intercompany_reconciliation.csv", ',', 500, FIRST,
                    "company_code:CODE", "partner_company:CODE", "gl_account:ID", "balance:AMOUNT",
                    "partner_balance:AMOUNT", "difference:AMOUNT", "currency:CUR", "reconciled:BOOL"),
            csv("Tax_codes.csv", ',', 40, FIRST,
                    "tax_code:TAX", "country:COUNTRY", "description:TEXT", "rate_percent:INT", "valid_from:DATE"),
            csv("Exchange_rates_TCURR_{period}.csv", ',', 400, FIRST,
                    "from_currency:CUR", "to_currency:CUR", "valid_from:DATE_DE", "rate:AMOUNT", "rate_type:CODE"),
            csv("Credit_memos.csv", ',', 600, FIRST,
                    "document_number:DOCNO", "customer_id:ID", "credit_date:DATE_DE", "amount:AMOUNT",
                    "currency:CUR", "reason:TEXT", "created_by:USER", "approved_by:USER"),
            csv("Manual_journal_approvals.csv", ',', 900, FIRST,
                    "document_number:DOCNO", "company_code:CODE", "amount:AMOUNT", "currency:CUR",
                    "prepared_by:USER", "approved_by:USER", "approved_at:TS", "self_approved:BOOL"),
            csv("VBAK_sales_orders.csv", ',', 1600, FIRST,
                    "sales_order:DOCNO", "customer_id:ID", "order_date:DATE_DE", "net_value:AMOUNT",
                    "currency:CUR", "sales_org:CODE", "created_by:USER"),
            csv("VBRK_billing_documents.csv", ',', 1700, FIRST,
                    "billing_document:DOCNO", "sales_order:DOCNO", "customer_id:ID", "billing_date:DATE_DE",
                    "net_value:AMOUNT", "tax_amount:AMOUNT", "currency:CUR", "cancelled:FLAG"),
            csv("Inventory_valuation.csv", ',', 1000, FIRST,
                    "material:ID", "plant:CODE", "stock_quantity:INT", "unit:UNIT", "moving_average_price:AMOUNT",
                    "stock_value:AMOUNT", "last_movement:DATE_DE"),
            csv("Contracts_register.csv", ',', 250, new Quarter(2024, 3),
                    "contract_id:ID", "counterparty:COMPANY", "order:DOCNO", "group:CODE", "start_date:DATE",
                    "end_date:DATE", "annual_value:AMOUNT", "owner:PERSON"),
            csv("Travel_expense_claims.csv", ',', 1200, FIRST,
                    "claim_id:DOCNO", "employee:PERSON", "submitted_on:DATE_DE", "amount:AMOUNT", "currency:CUR",
                    "category:CODE", "approved_by:PERSON", "paid:BOOL"),
            csv("SoD_conflicts.csv", ',', 350, FIRST,
                    "user_name:USER", "role_1:ROLE", "role_2:ROLE", "risk_id:CODE", "mitigated:BOOL",
                    "mitigating_control:TEXT"),
            csv("Firefighter_ID_usage_log.csv", '|', 300, new Quarter(2024, 2),
                    "firefighter_id:USER", "used_by:USER", "logon_at:TS", "logoff_at:TS", "reason:TEXT",
                    "reviewed_by:PERSON", "reviewed_on:DATE_DE"),
            csv("Vendor_bank_detail_changes.csv", ',', 200, FIRST,
                    "vendor_id:ID", "changed_at:TS", "changed_by:USER", "old_iban:IBAN", "new_iban:IBAN",
                    "confirmed_by_callback:BOOL"),
            csv("Accruals_and_provisions_manual_adjustments_finance_controlling_FINAL_v3.csv", ',', 150,
                    new Quarter(2025, 1),
                    "document_number:DOCNO", "company_code:CODE", "gl_account:ID", "amount:AMOUNT", "currency:CUR",
                    "reason:TEXT", "prepared_by:PERSON", "reviewed_by:PERSON", "posting_date:DATE_DE"),
            csv("Open_purchase_requisitions_awaiting_approval_by_cost_centre_owner.csv", ',', 400, new Quarter(2025, 3),
                    "requisition:DOCNO", "item:INT", "cost_center:ID", "requested_by:USER", "requested_on:DATE_DE",
                    "value:AMOUNT", "currency:CUR", "approver:PERSON"));

    private static final String[] CODES = {"DE01", "DE02", "AT01", "NL01", "FR03", "US01", "CH01"};
    private static final String[] USERS = {"JSCHMIDT", "MMUELLER", "AKOWALSKI", "LDUBOIS", "PJANSEN", "TWEBER",
            "SKRAUSE", "BATCH_FI", "RFC_CONNECT", "EVANDIJK", "HFISCHER", "CNOWAK"};
    private static final String[] PEOPLE = {"Julia Schmidt", "Marco Müller", "Anna Kowalski", "Louis Dubois",
            "Pieter Jansen", "Tobias Weber", "Sophie Krause", "Eva van Dijk", "Hannah Fischer", "Claudia Nowak"};
    private static final String[] WORDS = {"accrual", "reversal", "freight", "consulting", "licence", "rent",
            "Q-end adjustment", "intercompany", "rebate", "maintenance", "travel", "bonus", "correction", "write-off",
            "utilities", "insurance", "legal fees", "marketing"};
    private static final String[] COMPANY_A = {"Nordwind", "Alpen", "Rhein", "Hanse", "Delta", "Baltic", "Orion",
            "Vista", "Lumen", "Kestrel", "Brightwater", "Falkenberg"};
    private static final String[] COMPANY_B = {"Logistik GmbH", "Industrial Supplies B.V.", "Consulting AG",
            "Facility Services GmbH & Co. KG", "Software SAS", "Packaging Ltd", "Energie GmbH",
            "Holding International S.à r.l."};
    private static final String[] ROLES = {"Z_FI_AP_CLERK", "Z_FI_AP_APPROVER", "Z_FI_GL_ACCOUNTANT",
            "Z_FI_PAYMENT_RELEASE", "Z_MM_BUYER", "Z_MM_GOODS_RECEIPT", "Z_SD_BILLING", "Z_BASIS_ADMIN", "SAP_ALL"};
    private static final DateTimeFormatter GERMAN = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Extracts() {}

    static Extract csv(String fileName, char separator, int rows, Quarter since, String... columns) {
        return new Extract(fileName, separator, rows, since, Arrays.stream(columns).map(c -> {
            int colon = c.lastIndexOf(':');
            return new Column(c.substring(0, colon), Kind.valueOf(c.substring(colon + 1)));
        }).toList());
    }

    public static Extract byName(String fileName) {
        return ALL.stream().filter(e -> e.fileName().equals(fileName)).findFirst().orElseThrow();
    }

    public static List<String> header(Extract extract) {
        return extract.columns().stream().map(Column::header).toList();
    }

    public static List<List<String>> rows(Extract extract, Quarter q, int count) {
        Random random = new Random((extract.fileName() + q.label()).hashCode());
        List<List<String>> rows = new ArrayList<>(count);
        for (int r = 0; r < count; r++) {
            List<String> row = new ArrayList<>();
            for (Column column : extract.columns()) {
                row.add(value(column.kind(), q, r, random));
            }
            rows.add(row);
        }
        return rows;
    }

    /** Renders a CSV. {@code edit} may change the rows before they are written, e.g. to plant a bad value. */
    public static byte[] csvFile(Extract extract, Quarter q, int count, UnaryOperator<List<List<String>>> edit) {
        StringBuilder out = new StringBuilder();
        line(out, header(extract), extract.separator());
        for (List<String> row : edit.apply(rows(extract, q, count))) {
            line(out, row, extract.separator());
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder out, List<String> values, char separator) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(separator);
            }
            String v = values.get(i);
            boolean quote = v.indexOf(separator) >= 0 || v.indexOf('"') >= 0;
            out.append(quote ? '"' + v.replace("\"", "\"\"") + '"' : v);
        }
        out.append('\n');
    }

    /** A single-sheet Excel export of an extract. */
    public static byte[] singleSheetWorkbook(Extract extract, Quarter q, int count) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Export");
            write(sheet, header(extract), rows(extract, q, count));
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The finance team's cost centre workbook: several sheets, one empty, one with merged cells. */
    public static byte[] costCentreWorkbook(Quarter q, int scale) {
        Random random = new Random(q.label().hashCode());
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<List<String>> hierarchy = new ArrayList<>();
            List<List<String>> centres = new ArrayList<>();
            List<List<String>> profit = new ArrayList<>();
            List<List<String>> mapping = new ArrayList<>();
            for (int i = 0; i < 12 * scale; i++) {
                hierarchy.add(List.of("H" + (i / 4), "Node " + i, "H" + (i / 16), String.valueOf(i % 4 + 1)));
            }
            for (int i = 0; i < 40 * scale; i++) {
                String centre = "%010d".formatted(410000 + i * 10);
                centres.add(List.of(centre, WORDS[random.nextInt(WORDS.length)] + " " + (i + 1),
                        CODES[random.nextInt(CODES.length)], PEOPLE[random.nextInt(PEOPLE.length)],
                        q.start().minusDays(random.nextInt(900)).format(GERMAN)));
                mapping.add(List.of(centre, "%010d".formatted(90000 + i % 9 * 10)));
            }
            for (int i = 0; i < 9; i++) {
                profit.add(List.of("%010d".formatted(90000 + i * 10), "Profit centre " + (i + 1),
                        CODES[i % CODES.length]));
            }
            write(workbook.createSheet("Hierarchy"), List.of("node_id", "node_name", "parent_node", "level"), hierarchy);
            write(workbook.createSheet("Cost centres"),
                    List.of("cost_center", "description", "company_code", "responsible", "valid_from"), centres);
            write(workbook.createSheet("Profit centres"), List.of("profit_center", "description", "company_code"), profit);
            write(workbook.createSheet("Mapping"), List.of("cost_center", "profit_center"), mapping);
            workbook.createSheet("Notes");
            Sheet changes = workbook.createSheet("Changes " + q.label());
            List<List<String>> changeRows = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                changeRows.add(List.of("%010d".formatted(410000 + i * 10), i < 3 ? "Moved" : "", i < 3 ? "" : "Closed",
                        PEOPLE[i]));
            }
            write(changes, List.of("cost_center", "change", "change_detail", "requested_by"), changeRows);
            changes.addMergedRegion(new CellRangeAddress(1, 3, 2, 3));
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String costCentreWorkbookName(Quarter q) {
        return COST_CENTRE_WORKBOOK.replace("{year}", String.valueOf(q.year()));
    }

    private static void write(Sheet sheet, List<String> header, List<List<String>> rows) {
        Row head = sheet.createRow(0);
        for (int c = 0; c < header.size(); c++) {
            head.createCell(c).setCellValue(header.get(c));
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows.get(r).size(); c++) {
                row.createCell(c).setCellValue(rows.get(r).get(c));
            }
        }
    }

    private static String value(Kind kind, Quarter q, int row, Random random) {
        LocalDate day = q.start().plusDays(random.nextInt((int) (q.end().toEpochDay() - q.start().toEpochDay() + 1)));
        return switch (kind) {
            case CODE -> CODES[random.nextInt(CODES.length)];
            case DOCNO -> String.valueOf(5100000000L + q.year() * 10000L + row * 7L + random.nextInt(7));
            case YEAR -> String.valueOf(q.year());
            case DATE_DE -> day.format(GERMAN);
            case DATE -> day.toString();
            case TS -> day + " %02d:%02d:%02d".formatted(6 + random.nextInt(14), random.nextInt(60), random.nextInt(60));
            case USER -> USERS[random.nextInt(USERS.length)];
            case DOCTYPE -> new String[] {"SA", "KR", "KZ", "DR", "DZ", "AB"}[random.nextInt(6)];
            case TEXT -> WORDS[random.nextInt(WORDS.length)] + (random.nextInt(4) == 0
                    ? ", " + WORDS[random.nextInt(WORDS.length)] + " " + q.label() : "");
            case ID -> "%010d".formatted(100000 + random.nextInt(900000));
            case AMOUNT -> String.format(java.util.Locale.ROOT, "%.2f", (random.nextInt(10) == 0 ? -1 : 1) * random.nextDouble() * 50000);
            case CUR -> new String[] {"EUR", "EUR", "EUR", "USD", "GBP", "CHF"}[random.nextInt(6)];
            case DC -> random.nextBoolean() ? "S" : "H";
            case INT -> String.valueOf(1 + random.nextInt(999));
            case TAX -> new String[] {"V1", "V0", "A1", "E0"}[random.nextInt(4)];
            case UNIT -> new String[] {"EA", "KG", "H", "L"}[random.nextInt(4)];
            case COMPANY -> COMPANY_A[random.nextInt(COMPANY_A.length)] + " " + COMPANY_B[random.nextInt(COMPANY_B.length)];
            case COUNTRY -> new String[] {"DE", "AT", "NL", "FR", "US", "CH"}[random.nextInt(6)];
            case IBAN -> "DE%02d %04d %04d %04d %04d %02d".formatted(random.nextInt(100), random.nextInt(10000),
                    random.nextInt(10000), random.nextInt(10000), random.nextInt(10000), random.nextInt(100));
            case TERMS -> new String[] {"Z014", "Z030", "Z060", "Z090"}[random.nextInt(4)];
            case FLAG -> random.nextInt(8) == 0 ? "X" : "";
            case BOOL -> random.nextInt(5) == 0 ? "true" : "false";
            case PERSON -> PEOPLE[random.nextInt(PEOPLE.length)];
            case ROLE -> ROLES[random.nextInt(ROLES.length)];
            case EMAIL -> USERS[random.nextInt(USERS.length)].toLowerCase() + "@example.com";
            case STATUS -> new String[] {"OPEN", "OPEN", "PARKED", "BLOCKED"}[random.nextInt(4)];
        };
    }
}
