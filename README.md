# Spring Boot JWT Auth — RSA Rotation + Google Tink

Production-ready authentication base untuk Spring Boot. Dirancang stateless, aman, dan mudah dikustomisasi.

## Fitur Utama

- **JWT dengan RS256** — Access Token ditandatangani dengan RSA 2048-bit, bukan HMAC symmetric key
- **Automatic RSA Key Rotation** — Key pair dirotasi terjadwal via Quartz, token lama tetap valid selama grace period
- **Private Key Encryption dengan Google Tink** — Private key tidak pernah disimpan plaintext di DB; dienkripsi dengan AES-256-GCM (AEAD)
- **Opaque Refresh Token** — Format `sessionId.secret`, hanya hash SHA-256-nya yang disimpan di DB
- **Replay Attack Detection** — RT yang sudah dirotasi jika dipakai lagi akan langsung revoke semua session user
- **Authorities dari Redis, bukan JWT** — Role dan permission dimuat dari Redis per request; token tetap slim dan revocation instan
- **Rate Limiting Login** — Per-IP (fixed window) + per-username (exponential backoff) berbasis Redis, shared antar instance
- **JWKS Endpoint** — `/.well-known/jwks.json` untuk resource server lain memverifikasi JWT secara mandiri
- **i18n Error Messages** — Semua pesan error mendukung multi-bahasa (en / id included)

---

## Tech Stack

| Komponen | Library |
|---|---|
| JWT | Nimbus JOSE JWT |
| RSA Key Generation | Nimbus JOSE (`RSAKeyGenerator`) |
| Private Key Encryption | Google Tink AES-256-GCM |
| Scheduler | Quartz (clustered, JDBC store) |
| Cache | Redis (`StringRedisTemplate`) |
| Database | PostgreSQL + Flyway |
| Security | Spring Security 6 (stateless) |
| UUID | `uuid-creator` (time-ordered) |

---

## Arsitektur & Package Structure

```
auth/
├── delivery/http/
│   ├── AuthController.java          # /v1/app/auth — register, login, refresh, me
│   └── JwksController.java          # /.well-known/jwks.json
├── internal/
│   ├── entity/
│   │   ├── SigningKey.java           # RSA key pair di DB
│   │   ├── Credential.java          # Email + password hash per provider
│   │   └── RefreshSession.java      # Session + refresh token hash
│   ├── repo/                        # JPA Repositories
│   └── service/
│       └── LocalAuthService.java    # Login & register flow
└── security/
    ├── securityConfig.java          # Spring Security filter chain
    ├── JwtAuthenticationFilter.java # Extract + verify Bearer token per request
    ├── TokenService.java            # Issue & verify AT, issue & rotate RT
    ├── RsaKeyService.java           # Generate, rotate, load RSA key pair
    ├── PrivateKeyEncryptor.java     # Encrypt/decrypt private key via Tink AEAD
    ├── AuthorityCacheService.java   # Load roles dari Redis / DB fallback
    ├── SigningKeyCacheService.java  # Cache public key di Redis untuk verify JWT
    ├── SecurityBootstrap.java       # Ensure active key exists on startup
    ├── SecurityEventListener.java   # Revoke all sessions on replay attack
    ├── LoginRateLimiterService.java # Rate limit per-IP dan per-username
    └── scheduler/
        ├── KeyRotationJob.java      # Quartz job: rotate RSA key
        └── KeyRotationJobConfig.java
```

---

## Flow Lengkap

### 1. Register

```
POST /v1/app/auth/register
{ "email": "...", "password": "...", "nickname": "..." }
```

```
AuthController
  └── LocalAuthService.register()
        ├── UserService.registerUser()        → buat User + UserProfile di DB
        ├── Credential.save()                 → simpan email + bcrypt(password, cost=12)
        └── TokenService.issueTokenPair()     → return AT + RT
```

### 2. Login

```
POST /v1/app/auth/login
{ "email": "...", "password": "..." }
```

```
AuthController
  └── LocalAuthService.login()
        ├── LoginRateLimiterService.checkIpAllowed(ip)        → cek rate limit IP
        ├── LoginRateLimiterService.checkUsernameAllowed(email) → cek lockout username
        ├── CredentialRepo.findByProviderAndProviderId()      → ambil credential
        │     └── NOT FOUND → onLoginFailed() + throw 401
        ├── Cek UserStatus (ACTIVE / SUSPENDED / DISABLED)
        ├── bcrypt.matches(password, hash)
        │     └── FAIL → onLoginFailed() + throw 401
        ├── LoginRateLimiterService.onLoginSuccess()          → reset counter username
        └── TokenService.issueTokenPair()                     → return AT + RT
```

### 3. Setiap Request Terautentikasi

```
Request: Authorization: Bearer <AT>

JwtAuthenticationFilter
  ├── TokenService.verifyAccessToken(AT)
  │     ├── SignedJWT.parse() → ambil kid dari header
  │     ├── SigningKeyCacheService.getSigningKey(kid)
  │     │     └── Redis hit → return SigningKey (cache TTL 10 hari)
  │     │     └── Redis miss → DB lookup → simpan ke Redis
  │     ├── RSASSAVerifier.verify() → validasi signature RS256
  │     └── cek exp claim
  └── AuthorityCacheService.getAuthorities(userId)
        └── Redis hit  → cek status user → return roles
        └── Redis miss → DB lookup → simpan ke Redis (TTL = 15 menit)
              └── cek UserStatus (SUSPENDED/DISABLED → throw 403)
```

### 4. Refresh Token

```
POST /v1/app/auth/refresh
{ "refreshToken": "<sessionId>.<rawSecret>" }
```

```
TokenService.rotateRefreshToken()
  ├── split RT → sessionId + rawSecret
  ├── findBySessionId() → ambil session dari DB
  ├── hashOpaque(rawSecret) → compare dengan hash di DB (constant-time)
  ├── cek isRevoked
  │     └── TRUE → publishEvent(ReplayAttackDetectedEvent)
  │           └── SecurityEventListener → revokeAllByUserId() di transaksi baru
  ├── cek expiresAt
  ├── set session lama isRevoked = true, revokeReason = "ROTATED_REFRESH_TOKEN"
  └── issueTokenPair() → return AT + RT baru
```

### 5. RSA Key Rotation

```
KeyRotationJob (Quartz, terjadwal)
  └── RsaKeyService.rotateKey()
        ├── generateRsaKeyPair()              → RSA 2048-bit via Nimbus
        ├── PrivateKeyEncryptor.encrypt()     → AES-256-GCM via Tink
        ├── signingKeyRepo.deactivateAllActiveKeys() → key lama jadi inactive
        ├── SigningKeyCacheService.evictAllSigningKeyCache() → bersihkan Redis
        ├── signingKeyRepo.save(newKey)       → simpan key baru
        └── signingKeyRepo.deleteExpiredKeys() → hapus key yang sudah lewat expiresAt
```

> **Grace period**: Key lama tidak langsung dihapus. `expiresAt` = waktu key boleh dihapus (default 30 hari setelah dibuat). Selama grace period, token lama yang masih valid tetap bisa diverifikasi. Pastikan `RSA_ROTATION_TTL` > `ACCESS_TOKEN_TTL`.

### 6. Rate Limiting

Dua counter independen di Redis:

**Per-IP** (credential stuffing guard):
```
Key: rate:login:ip:{ip}
Limit: 10 request / 60 detik (fixed window)
Reset: TIDAK direset saat login sukses
```

**Per-username** (brute force guard):
```
Key (counter): rate:login:user:fails:{username}   → TTL 24 jam, TIDAK ikut expire saat lockout habis
Key (lockout):  rate:login:user:lockout:{username} → TTL = durasi lockout aktif

Gagal ke-5  → lockout 60 detik
Gagal ke-6  → lockout 120 detik  (counter tetap 6 karena key fails tidak dihapus)
Gagal ke-7  → lockout 240 detik
...cap 1 jam

Reset: DIRESET saat login sukses (hapus kedua key)
```

---

## Setup & Konfigurasi

### 1. Environment Variables

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=bayr
DB_USERNAME=bayr_user
DB_PASSWORD=secret

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_secret
REDIS_SSL=false

# JWT
AUTH_ACCESS_TOKEN_TTL=15m
AUTH_REFRESH_TOKEN_TTL=15d
AUTH_RSA_ROTATION_TTL=30d   # harus > ACCESS_TOKEN_TTL

# Google Tink — generate sekali, simpan di secrets manager
TINK_KEYSET_JSON=<generate dengan cara di bawah>
```

### 2. Generate Tink Keyset

Jalankan sekali saja (via unit test atau main method sementara):

```java
String keysetJson = PrivateKeyEncryptor.generateNewKeysetJson();
System.out.println(keysetJson);
```

Simpan output-nya ke secrets manager (AWS Secrets Manager, Vault, dll). **Jangan pernah commit ke source control.**

> ⚠️ Jika keyset ini hilang, semua private key di DB tidak bisa didekripsi. Backup keyset dengan aman.

### 3. Database Migration

Project menggunakan Flyway. Jalankan migration sebelum start:

```bash
./mvnw flyway:migrate
```

### 4. Quartz — Jadwal Rotasi Key

Default konfigurasi di `KeyRotationJobConfig.java`. Ubah cron expression sesuai kebutuhan:

```java
// Contoh: setiap hari pukul 02:00
CronScheduleBuilder.cronSchedule("0 0 2 * * ?")
```

---

## API Endpoints

| Method | Path | Auth | Deskripsi |
|---|---|---|---|
| `POST` | `/v1/app/auth/register` | Public | Registrasi user baru |
| `POST` | `/v1/app/auth/login` | Public | Login, return AT + RT |
| `POST` | `/v1/app/auth/refresh` | Public | Rotate refresh token |
| `GET` | `/v1/app/auth/me` | Bearer AT | Data user yang sedang login |
| `GET` | `/.well-known/jwks.json` | Public | JWKS untuk verifikasi JWT eksternal |

### Contoh Response Login

```json
{
  "message": "Login successful",
  "data": {
    "accessToken": "eyJraWQ...",
    "refreshToken": "01935c2a-...<sessionId>.<rawSecret>"
  }
}
```

### Contoh Response Error Rate Limit (429)

```json
{
  "message": "Terlalu banyak percobaan. Coba lagi dalam 120 detik.",
  "errors": null
}
```

---

## Security Design Decisions

### Kenapa Role tidak di JWT claims?

Menyimpan role/permission di JWT berarti:
- Token membengkak setiap request
- Tidak bisa revoke permission secara instan (harus tunggu token expire)

Solusi di project ini: JWT hanya berisi `sub` (userId) + `exp`. Role dimuat dari Redis per request dengan TTL = AT TTL. Untuk revoke permission user, cukup evict key Redis-nya.

### Kenapa private key dienkripsi dengan Tink?

Private key RSA tidak boleh disimpan plaintext di DB. Tink menyediakan:
- **AES-256-GCM (AEAD)**: Authenticated Encryption — ciphertext tidak bisa dimanipulasi tanpa terdeteksi
- **Associated Data** (`signing_key_private`): konteks tambahan yang diverifikasi saat decrypt, tanpa ikut dienkripsi
- **Key rotation-ready**: Tink keyset bisa berisi multiple key, memudahkan rotasi keyset tanpa downtime

### Kenapa Opaque Refresh Token, bukan JWT?

RT tidak perlu membawa claim apapun — tugasnya hanya sebagai "tiket" untuk dapat AT baru. Format opaque `sessionId.secret`:
- `sessionId` (UUID time-ordered): untuk lookup O(1) ke DB tanpa full table scan
- `rawSecret` (16-byte random): diverifikasi dengan constant-time compare setelah di-hash SHA-256

### Replay Attack Protection

Jika RT yang sudah dirotasi (status `isRevoked = true`) dipakai kembali, sistem mendeteksi ini sebagai replay attack dan **langsung merevoke semua session** milik user tersebut. Event ini diproses di transaksi terpisah (`REQUIRES_NEW`) agar tetap jalan walau transaksi utama rollback.

---

## Kustomisasi

### Menambah Permission (selain Role)

Di `AuthorityCacheService.getAuthorities()`, saat ini hanya memuat roles. Untuk menambah granular permission:

```java
// Ganti bagian ini:
Set<String> roles = userRes.roles().stream()
    .map(role -> "ROLE_" + role.code().toUpperCase())
    .collect(Collectors.toSet());

// Jadi:
Set<String> authorities = new HashSet<>();
userRes.roles().forEach(r -> authorities.add("ROLE_" + r.code().toUpperCase()));
userRes.permissions().forEach(p -> authorities.add(p.code())); // tambahkan permission
```

### Mengubah AT / RT TTL

Via environment variable atau langsung di `application.yaml`:

```yaml
app:
  jwt:
    access-token-ttl: 30m   # default 15m
    refresh-token-ttl: 7d   # default 15d
```

### Mengubah Threshold Rate Limit

Di `LoginRateLimiterService.java`:

```java
private static final int  IP_MAX_ATTEMPTS     = 10;   // max hit per IP per window
private static final long IP_WINDOW_SECONDS   = 60L;  // window dalam detik

private static final int  USER_MAX_FAILS      = 5;    // max gagal sebelum lockout
private static final long LOCKOUT_BASE_SECONDS = 60L; // lockout pertama
private static final long LOCKOUT_MAX_SECONDS  = 3600L; // cap lockout
```

### Menambah OAuth2 Provider

Tambahkan value baru di `AuthProviderType.java` dan buat service terpisah (mirip `LocalAuthService`) yang memanggil `TokenService.issueTokenPair()` setelah verifikasi token dari provider.

---

## Catatan Deployment

- **Redis wajib running** sebelum aplikasi start. Rate limiter dan authority cache bergantung pada Redis. Jika Redis down, rate limiter fail-open (tidak memblokir login), tapi authority cache akan fallback ke DB setiap request.
- **Multi-instance**: Quartz dikonfigurasi clustered (`isClustered: true`) sehingga key rotation hanya dijalankan oleh satu instance. Redis dipakai bersama antar instance — tidak perlu sticky session.
- **Backup Tink Keyset**: Simpan di secrets manager (bukan di kode atau `.env` file yang di-commit). Rotasi keyset Tink dilakukan manual jika diperlukan.
- **`RSA_ROTATION_TTL` harus lebih besar dari `ACCESS_TOKEN_TTL`**: Agar token yang dibuat sesaat sebelum rotasi tetap bisa diverifikasi selama masa aktifnya.