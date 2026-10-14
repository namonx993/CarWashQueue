import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * จัดการคิว: กติกาการจอง (ทะเบียน วันที่ ห้ามย้อนหลัง ช่วงเวลาเต็ม) เพิ่ม/ยกเลิก/เปลี่ยนสถานะ/ลบ
 * สรุปรายได้ จัดการราคาบริการ และอ่าน/เขียนไฟล์ .txt อัตโนมัติทุกครั้งที่ข้อมูลเปลี่ยน
 * ผิดเงื่อนไขจะโยน IllegalArgumentException พร้อมข้อความภาษาไทย — ไม่เกี่ยวกับหน้าจอ
 */
public class QueueManager {
    public static final String[] CARS = {"มอเตอร์ไซค์", "รถเก๋ง", "SUV", "กระบะ", "รถตู้"};
    public static final double[] MULT = {0.5, 1, 1.2, 1.3, 1.5}; // ตัวคูณราคา เรียงตรงกับ CARS
    public static final String[] SLOTS = {"09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
    public static final String WAIT = "รอดำเนินการ", DONE = "เสร็จสิ้น", CANCEL = "ยกเลิก";
    public static final int BAYS = 2; // รถที่รับได้พร้อมกันต่อช่วงเวลา

    private final Path bookFile, priceFile;
    private final Map<String, Integer> prices = new LinkedHashMap<>();
    private final List<CUSTOMER> items = new ArrayList<>();

    public QueueManager(String bookFileName, String priceFileName) {
        bookFile = Paths.get(bookFileName);
        priceFile = Paths.get(priceFileName);
        loadPrices();
        loadBookings();
    }

    // ---------- โหลดข้อมูล ----------

    /** โหลดราคาจากไฟล์ ถ้ายังไม่มีไฟล์ให้สร้างราคาเริ่มต้นแล้วบันทึก */
    private void loadPrices() {
        if (!Files.exists(priceFile)) {
            prices.put("ล้างสีภายนอก", 100);
            prices.put("ล้าง+ดูดฝุ่น", 180);
            prices.put("ล้าง+เคลือบสี", 350);
            prices.put("ล้างห้องเครื่อง", 250);
            savePrices();
            return;
        }
        for (String line : readLines(priceFile)) {
            String[] p = line.split(",");
            if (p.length != 2) continue;
            try { prices.put(p[0], Integer.parseInt(p[1].trim())); }
            catch (NumberFormatException ignore) { } // ข้ามบรรทัดเสีย
        }
    }

    private void loadBookings() {
        for (String line : readLines(bookFile)) {
            if (line.trim().isEmpty()) continue;
            try { items.add(CUSTOMER.fromLine(line)); }
            catch (RuntimeException ignore) { } // ข้ามบรรทัดเสีย
        }
    }

    // ---------- ราคาบริการ ----------

    public Set<String> names() { return prices.keySet(); }

    public int basePrice(String service) { return prices.get(service); }

    public int calc(String service, int carIdx) { return (int) Math.round(prices.get(service) * MULT[carIdx]); }

    public void setPrice(String service, int price) {
        if (price < 0) throw new IllegalArgumentException("ราคาต้องไม่ติดลบ");
        prices.put(service, price);
    }

    public void savePrices() {
        write(priceFile, prices.entrySet().stream()
                .map(e -> e.getKey() + "," + e.getValue())
                .collect(Collectors.toList()));
    }

    // ---------- การจอง ----------

    /** ตัดจุลภาคออก (เพราะไฟล์ใช้ , คั่นข้อมูล) และตัดช่องว่างหัวท้าย */
    public static String clean(String s) { return s.replace(",", " ").trim(); }

    public CUSTOMER book(String name, String phone, String plate, int carIdx,
                         String service, String dateText, String time) {
        plate = clean(plate);
        if (plate.isEmpty()) throw new IllegalArgumentException("กรุณากรอกทะเบียนรถ");

        LocalDate date = parseDate(dateText);
        if (LocalDateTime.of(date, LocalTime.parse(time)).isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("ไม่สามารถจองเวลาที่ผ่านมาแล้ว");

        String day = date.toString();
        if (bookedCount(day, time) >= BAYS)
            throw new IllegalArgumentException("ช่วงเวลานี้เต็มแล้ว กรุณาเลือกเวลาอื่น");

        int id = items.stream().mapToInt(b -> b.id).max().orElse(0) + 1;
        CUSTOMER c = new CUSTOMER(id, clean(name), phone, plate, CARS[carIdx], service, day, time,
                calc(service, carIdx), WAIT);
        items.add(c);
        saveBookings();
        return c;
    }

    public void cancelByCustomer(int id, String phone) {
        CUSTOMER c = get(id);
        if (!c.phone.equals(phone) || !c.status.equals(WAIT))
            throw new IllegalArgumentException("ยกเลิกได้เฉพาะรายการของคุณที่รอดำเนินการ");
        c.status = CANCEL;
        saveBookings();
    }

    public void setStatus(int id, String status) { get(id).status = status; saveBookings(); }

    public void delete(int id) { items.remove(get(id)); saveBookings(); }

    public List<CUSTOMER> all() { return Collections.unmodifiableList(items); }

    public List<CUSTOMER> byPhone(String phone) {
        return items.stream().filter(b -> b.phone.equals(phone)).collect(Collectors.toList());
    }

    // ---------- รายได้ (นับเฉพาะรายการที่เสร็จสิ้น) ----------

    public int incomeOn(String date) {
        return done().filter(b -> b.date.equals(date)).mapToInt(b -> b.price).sum();
    }

    public int countDoneOn(String date) {
        return (int) done().filter(b -> b.date.equals(date)).count();
    }

    public int incomeTotal() {
        return done().mapToInt(b -> b.price).sum();
    }

    // ---------- ภายใน ----------

    private Stream<CUSTOMER> done() {
        return items.stream().filter(b -> b.status.equals(DONE));
    }

    /** จำนวนการจองที่ยังใช้ช่องบริการอยู่ในวัน/เวลานั้น (ไม่นับที่ยกเลิก) */
    private long bookedCount(String day, String time) {
        return items.stream()
                .filter(b -> b.date.equals(day) && b.time.equals(time) && !b.status.equals(CANCEL))
                .count();
    }

    private static LocalDate parseDate(String text) {
        try { return LocalDate.parse(text.trim()); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("รูปแบบวันที่ไม่ถูกต้อง (yyyy-MM-dd)"); }
    }

    private CUSTOMER get(int id) {
        return items.stream().filter(b -> b.id == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบรายการจอง"));
    }

    private void saveBookings() {
        write(bookFile, items.stream().map(CUSTOMER::toLine).collect(Collectors.toList()));
    }

    /** อ่านทุกบรรทัดของไฟล์ ถ้าไม่มีไฟล์คืนรายการว่าง */
    private static List<String> readLines(Path file) {
        if (!Files.exists(file)) return Collections.emptyList();
        try { return Files.readAllLines(file, StandardCharsets.UTF_8); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    private static void write(Path file, List<String> lines) {
        try { Files.write(file, lines, StandardCharsets.UTF_8); }
        catch (IOException e) { throw new IllegalStateException("บันทึกไฟล์ไม่สำเร็จ: " + e.getMessage()); }
    }
}