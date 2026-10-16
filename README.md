# CarWashQueue
> ระบบจัดการและจองศูนย์คิวบริการล้างรถ

## ⚙️ รายชื่อสมาชิกในกลุ่ม และ การแบ่งหน้าที่

| ชื่อ - สกุล | รหัสนิสิต | หน้าที่รับผิดชอบ | คลาสที่ดูแล |
| :--- | :---: | :--- | :--- |
| **พชร ปานเพ็ชร์** | 6721651564 | ออกแบบโครงสร้างข้อมูล และระบบรัน | `CarWashRunner.java`<br>`CUSTOMER.java` |
| **ปวริศ บำรุงกิจ** | 6721651521 | จัดการระบบคิวและระบบบันทึกไฟล์อัตโนมัติ | `QueueManager.java` |
| **นิธาน อมรศิลปพิทักษ์** | 6721651491 | ระบบบันทึกไฟล์อัตโนมัติและออกแบบหน้าตาโปรแกรม GUI (Layout) | `QueueManager.java`<br>`CarWashUI.java` |
| **ภูริณัฐ บุญศรี** | 6721651696 | ควบคุมระบบอีเวนต์ปุ่มกด และ Dropdown แพ็กเกจ | `CarWashUI.java` |

-------------------------------------
// ============================================================
// QueueManager.java
// ============================================================

// ---------- บรรทัดบนสุดของคลาส (ค่าคงที่) ----------

// [เพิ่มประเภทรถใหม่ "รถบรรทุก" คิดราคา x2.0]
// เพิ่มค่าใน Array CARS และ MULT ให้ตรงตำแหน่งกัน
// public static final String[] CARS = {"มอเตอร์ไซค์", "รถเก๋ง", "SUV", "กระบะ", "รถตู้", "รถบรรทุก"};
// public static final double[] MULT = {0.5, 1, 1.2, 1.3, 1.5, 2.0};

// [เพิ่มสถานะคิว "กำลังล้าง"]
// เพิ่มตัวแปร
// public static final String WASHING = "กำลังล้าง";
// (ในตารางต้นฉบับเขียนเป็น CANCEL = "ยกเลิก", WASH = "กำลังล้าง" -> ใช้ชื่อให้ตรงกันทั้งโปรเจกต์ เช่น WASHING)

// [ปรับจำนวนรถที่รับได้พร้อมกัน (Capacity)]
// เปลี่ยน public static final int BAYS = 2; เป็น BAYS = 3;
// (UI ฝั่งลูกค้าจะคำนวณ "ว่าง X คัน" ใหม่ตามตัวเลขนี้อัตโนมัติ)

// [เพิ่มเวลาเปิด-ปิดร้าน 08:00 - 18:00]
// แก้ในอาร์เรย์ SLOTS: เพิ่ม "08:00" ไว้ข้างหน้า และ "18:00" ไว้ข้างหลัง
// (เวลาในหน้าจอจองคิวของลูกค้าจะงอกขึ้นมาเองอัตโนมัติ)


// ---------- เมธอด book() ----------

// [1. Validation: ชื่อลูกค้าว่าง]
// เพิ่มเงื่อนไขตรวจสอบชื่อก่อนสร้าง Object CUSTOMER
// if (clean(name).isEmpty()) {
//     throw new IllegalArgumentException("กรุณากรอกชื่อลูกค้า");
// }

// [2. Validation: ทะเบียนรถต้องยาวอย่างน้อย 3 ตัวอักษร]
// if (plate.length() < 3) throw new IllegalArgumentException("ทะเบียนสั้นเกินไป");

// [3. ห้ามเลือกวันที่ย้อนหลัง (แยกจากการเช็คเวลา)]
// if (d.isBefore(LocalDate.now())) throw new IllegalArgumentException("ห้ามจองวันที่ย้อนหลัง");

// [4. ต้องจองล่วงหน้าอย่างน้อย 1 ชั่วโมง]
// ปรับเงื่อนไขการเช็คเวลา:
// LocalDateTime bookTime = LocalDateTime.of(d, LocalTime.parse(time));
// if (bookTime.isBefore(LocalDateTime.now().plusHours(1))) {
//     throw new IllegalArgumentException("ต้องจองล่วงหน้าอย่างน้อย 1 ชั่วโมง");
// }

// [5. ราคาวันเสาร์-อาทิตย์ +20%]
// แก้ใน book() เท่านั้น: คำค้น calc(service, carIdx), WAIT
// ให้คำนวณก่อนบรรทัด CUSTOMER c =
// int price = calc(service, carIdx);
// if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY)
//     price = (int) Math.round(price * 1.2);

// [6. ลดตามจำนวนครั้งที่ล้างเสร็จ (เช่น ครบ 5 ครั้ง ลด 10%)]
// คำค้น calc(service, carIdx), WAIT -> เปลี่ยนเป็น calc(service, carIdx, phone), WAIT

// [7. ลูกค้าเก่า (เบอร์โทรเดิมเคยล้างเสร็จแล้วอย่างน้อย 1 ครั้ง) ลดราคางานใหม่ 10%]
// จุดที่ต้องแก้: ก่อนบรรทัดสร้าง CUSTOMER c =
// int finalPrice = calc(service, carIdx);
// boolean isOldCustomer = items.stream().anyMatch(b -> b.phone.equals(phone) && b.status.equals(DONE));
// if (isOldCustomer) {
//     finalPrice = (int)(finalPrice * 0.9); // ลด 10%
// }
// // แก้ parameter ตอนสร้าง CUSTOMER ให้ใช้ finalPrice แทน
// CUSTOMER c = new CUSTOMER(id, clean(name), phone, plate, CARS[carIdx], service, day, time, finalPrice, WAIT);

// [8. เพิ่มฟิลด์ใหม่ "สีรถ" (color)]
// เพิ่ม Parameter color ลงในเมธอด book() และส่งต่อไปยัง constructor ของ CUSTOMER


// ---------- เมธอด calc() ----------

// [โปรโมชัน: บริการ "ล้างห้องเครื่อง" (250 บาท) ลดราคาลง 50 บาท ทุกประเภทรถ]
// เพิ่มเงื่อนไขก่อน return ค่า:
// if (service.equals("ล้างห้องเครื่อง")) return price - 50;

// [ลดตามจำนวนครั้งที่ล้างเสร็จ] เพิ่ม overload รับ phone
// public int doneCount(String phone) {
//     return (int) items.stream().filter(b -> b.phone.equals(phone) && b.status.equals(DONE)).count();
// }
// public int calc(String service, int carIdx, String phone) {
//     int p = calc(service, carIdx);
//     return doneCount(phone) >= 5 ? (int) Math.round(p * 0.9) : p;
// }


// ---------- เมธอดใหม่ที่ต้องเพิ่ม ----------

// [รายได้รายเดือน] คำค้น public int incomeTotal() แล้วเติมเมธอดนี้ต่อ
// public int incomeMonth(String ym) { // เช่น "2026-10"
//     return items.stream()
//         .filter(b -> b.status.equals(DONE) && b.date.startsWith(ym))
//         .mapToInt(b -> b.price).sum();
// }

// [บริการยอดนิยม] เติมเมธอด (ถ้ายาวเกินให้ใช้ JLabel ตัวอักษร 18f)
// public String topService() {
//     return items.stream().filter(b -> b.status.equals(DONE))
//         .collect(Collectors.groupingBy(b -> b.service, Collectors.counting()))
//         .entrySet().stream().max(Map.Entry.comparingByValue())
//         .map(Map.Entry::getKey).orElse("-");
// }

// [สถิติแยกตามประเภทรถ (Categorized Statistics)] ใช้ Stream จัดกลุ่ม
// public long countDoneByCar(String date, String carType) {
//     return items.stream()
//         .filter(b -> b.date.equals(date) && b.status.equals(DONE) && b.car.equals(carType))
//         .count();
// }

// [ปุ่ม "ล้างประวัติการยกเลิก"] Logic
// public void clearCanceled() {
//     items.removeIf(b -> b.status.equals(CANCEL));
//     saveBookings();
// }


// ============================================================
// OwnerScreen.java
// ============================================================

// ---------- คลาส BookingTab ----------

// [เพิ่มสถานะ "กำลังล้าง"]
// 1) เพิ่มปุ่ม "กำลังล้าง" สีฟ้า ข้าง ๆ ปุ่ม "เสร็จสิ้น"
//    (เติมก่อน) JButton washButton = new CarWashUI.RoundButton("เริ่มล้าง", BLUE);
// 2) เพิ่ม Action ให้ปุ่มเรียก changeSelectedStatus(QueueManager.WASHING)
//    washButton.addActionListener(e -> changeSelectedStatus(QueueManager.WASHING));
// 3) buttons.add(washButton); // เติมก่อน buttons.add(doneButton);
// 4) เพิ่ม Status นี้ลงใน statusBox สำหรับฟิลเตอร์ค้นหา
//    new String[]{ALL, QueueManager.WAIT, QueueManager.WASHING, QueueManager.DONE, QueueManager.CANCEL}
//    (เติม QueueManager.WASHING ก่อน QueueManager.DONE)

// [ปุ่ม "ล้างประวัติการยกเลิก" สำหรับเจ้าของร้าน]
// ใน buildActionButtons() สร้างปุ่มใหม่ แอด Listener เรียก qm.clearCanceled(); แล้วสั่ง reload(); ตาราง

// [เรียงข้อมูลเริ่มต้น (Default Sorting)] ในเมธอด reload()
// ก่อนจะโหลดแถวลงตาราง ให้ Reverse ข้อมูลก่อน:
// List<CUSTOMER> allItems = new ArrayList<>(qm.all());
// Collections.reverse(allItems);
// CarWashUI.loadRows(model, allItems);

// [UI ขั้นสูง: ดับเบิลคลิกเปลี่ยนสถานะ] ตอนสร้าง Tab create()
// เพิ่ม Event ให้ตาราง:
// table.addMouseListener(new java.awt.event.MouseAdapter() {
//     public void mouseClicked(java.awt.event.MouseEvent e) {
//         if (e.getClickCount() == 2) { // ดับเบิลคลิก
//             changeSelectedStatus(QueueManager.DONE);
//         }
//     }
// });

// [ยอดรวมของรายการที่กำลังกรองอยู่]
// ความยาก: ต้องดึงจาก table ที่กำลังแสดงผลอยู่ ไม่ใช่จาก model (ไม่งั้นได้ยอดของทุกคัน)
// จุดที่ต้องแก้ 1: เพิ่มตัวแปร
// JLabel sumFilteredLabel = CarWashUI.createLabel("รวม: 0 บาท", 13f, Font.BOLD, GREEN);
// จุดที่ต้องแก้ 2: ในเมธอด applyFilter() เพิ่มลูปคำนวณเงินจากหน้าจอ
// int total = 0;
// for (int i = 0; i < table.getRowCount(); i++) {
//     // เช็คสถานะก่อนบวก หรือบวกหมดตามที่กรองมา
//     total += (Integer) table.getValueAt(i, CarWashUI.COLUMNS.length - 2); // คอลัมน์ราคาคือ 8
// }
// sumFilteredLabel.setText(String.format("รวม: %,d บาท", total));

// ---------- คลาส IncomeTab ----------

// [รายได้รายเดือน]
// - เติมต่อจาก private final JLabel totalIncomeLabel:
//   private final JLabel monthIncomeLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, GREEN);
// - คำค้น new GridLayout(1, 4, 16, 0) -> new GridLayout(1, 5, 16, 0)
// - คำค้น stats.add(createStatCard("รายได้รวมทั้งหมด" เติมก่อน:
//   stats.add(createStatCard("รายได้ (เดือนที่เลือก)", monthIncomeLabel));
// - ใน refresh() คำค้น totalIncomeLabel.setText เติมก่อน:
//   String ym = day.length() >= 7 ? day.substring(0, 7) : day;
//   monthIncomeLabel.setText(String.format("%,d บาท", qm.incomeMonth(ym)));

// [สถิติแยกตามประเภท] เพิ่ม Label ใหม่ใน IncomeTab และเรียกใช้ qm.countDoneByCar(...) / qm.topService()

// [ปุ่ม Export รายงานของวันนี้ เป็นไฟล์ report.txt แยกต่างหาก] ใน IncomeTab
// JButton exportBtn = new CarWashUI.RoundButton("Export", BLUE);
// exportBtn.addActionListener(e -> {
//     String day = dayField.getText().trim();
//     List<String> lines = qm.all().stream()
//         .filter(b -> b.date.equals(day) && b.status.equals(QueueManager.DONE))
//         .map(b -> b.toLine()) // ดึงข้อมูลทุกฟิลด์
//         .collect(Collectors.toList());
//     try {
//         Files.write(Paths.get("report_" + day + ".txt"), lines, StandardCharsets.UTF_8);
//         ui.showInfo("ส่งออกไฟล์สำเร็จ");
//     } catch (IOException ex) {
//         ui.showWarning("ส่งออกล้มเหลว");
//     }
// });


// ============================================================
// CarWashUI.java
// ============================================================

// [ลดตามจำนวนครั้ง] ใน updatePrice() คำค้น
// qm.calc(service, carTypeBox.getSelectedIndex())
//   -> qm.calc(service, carTypeBox.getSelectedIndex(), customerPhone)

// [เบอร์โทรต้องขึ้นต้นด้วย 0 เท่านั้น] ในเมธอด buildCustomerLoginForm()
// เปลี่ยน Regex จาก "\\d{9,10}" เป็น ^0\\d{8,9}$
// boolean phoneValid = phone.matches("^0\\d{8,9}$");

// [สถานะกำลังล้าง] เพิ่มต่อจาก else if (status.equals(QueueManager.CANCEL)) setForeground(RED);
// else if (status.equals(QueueManager.WASHING)) setForeground(BLUE);

// [เพิ่มฟิลด์ "สีรถ"]
// เพิ่มช่องกรอกข้อความ colorField และเพิ่มคอลัมน์ "สีรถ" ใน COLUMNS


// ============================================================
// CUSTOMER.java
// ============================================================

// [เพิ่มฟิลด์ "สีรถ"] (กระทบทุกไฟล์: CUSTOMER / CarWashUI / QueueManager)
// เพิ่มตัวแปร color ลงใน Constructor, toLine(), fromLine() และ toRow()
