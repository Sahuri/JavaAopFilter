# Spring Boot API Audit Logger

Contoh Spring AOP dan penggunaan Filter di java code untuk mencatat log audit (URL, HTTP Status, dan Response Body) secara otomatis pada endpoint yang dipasangi anotasi `@LogData`. Sudah mendukung proses asynchronous (`CompletableFuture`).

## 🚀 Fitur Utama
- **Support Async**: Log body tetap aman tercatat walaupun menggunakan `CompletableFuture`.
- **Ekspresi SpEL**: Bisa membaca field dinamis dari Request Body Map (Contoh: `#body['orderId']`).
- **Aman Memory**: Pembersihan context (`MDC.clear()`) dikelola otomatis agar tidak bocor.

## 🛠️ Cara Penggunaan

Cukup pasang anotasi `@LogData` di atas method Controller Anda:

```java
@PostMapping("/submit")
@LogData(traceId = "#body['orderId']", actionType = "submit-order")
public CompletableFuture<ResponseEntity<Map<String, Object>>> submitOrder(
        @RequestBody Map<String, Object> body) {
    
    return CompletableFuture.supplyAsync(() -> {
        body.put("status", "paid");
        return new ResponseEntity<>(body, HttpStatus.OK);
    });
}
```
### 📋 Contoh Hasil Log

Setiap kali API diakses, sistem akan otomatis mencetak log terstruktur seperti di bawah ini:

```text
2026-05-16 17:45:10 [INFO] --- LOG AUDIT ASYNC ---
2026-05-16 17:45:10 [INFO] actionType : submit-order
2026-05-16 17:45:10 [INFO] traceId    : ORD-99281
2026-05-16 17:45:10 [INFO] url        : /api/v1/submit
2026-05-16 17:45:10 [INFO] status     : 200
2026-05-16 17:45:10 [INFO] body       : {"orderId":"ORD-99281","status":"paid"}
2026-05-16 17:45:10 [INFO] -----------------------
```

## ⚙️ Configuration & Installation

### Prerequisites
* **Java 11** atau versi di atasnya
* **Spring Boot 2.x / 3.x**
* **Lombok** (Opsional, digunakan untuk merapikan anotasi log)
