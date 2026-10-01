/**
 * Model: ข้อมูลการจอง 1 รายการ (รหัส ชื่อ โทร ทะเบียน ประเภทรถ แพ็กเกจ วันที่ เวลา ราคา สถานะ)
 * แปลงเป็น/จากบรรทัดในไฟล์ .txt และเป็นแถวของตาราง — ไม่มีกติกาธุรกิจ ไม่แตะไฟล์ ไม่แสดงผล
 */
public class CUSTOMER {
    public final int id, price;
    public final String name, phone, plate, car, service, date, time;
    public String status;

    public CUSTOMER(int id, String name, String phone, String plate, String car,
                    String service, String date, String time, int price, String status) {
        this.id = id; this.name = name; this.phone = phone; this.plate = plate; this.car = car;
        this.service = service; this.date = date; this.time = time; this.price = price; this.status = status;
    }

    public String toLine() {
        return String.join(",", "" + id, name, phone, plate, car, service, date, time, "" + price, status);
    }

    /** แปลงบรรทัดเป็นออบเจ็กต์ ถ้าบรรทัดเสียจะโยน RuntimeException */
    public static CUSTOMER fromLine(String line) {
        String[] p = line.split(",", -1);
        return new CUSTOMER(Integer.parseInt(p[0]), p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                Integer.parseInt(p[8]), p[9]);
    }

    public Object[] toRow() {
        return new Object[]{id, name, phone, plate, car, service, date, time, price, status};
    }
}
