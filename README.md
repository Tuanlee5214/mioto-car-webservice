# mioto-car-webservice

Tầng trình diễn của Mioto Car - embedded Jetty, expose REST/JSON cho frontend. Không dùng web.xml, servlet đăng ký thẳng trong code (`apps/HttpServer.java`). Service này không chạm DB, mọi nghiệp vụ gọi qua Thrift sang `mioto-car-middleware` (client gói trong `mioto-car-clientwrapper.jar`, build từ repo `mioto-car-wrapperclient`).

## Luồng chạy

```
Browser/FE (localhost:5173 lúc dev)
      │  HTTP + JSON, cookie session
      ▼
HttpServer (embedded Jetty)
      │
*Servlet extends BaseServlet
      │   AuthServlet            → mọi request bắt buộc login
      │   AuthWithoutGetServlet  → GET public, POST/PUT/DELETE cần login
      │
ClientHolder (Thrift client sang middleware)
      ▼
mioto-car-middleware
```

`BaseServlet` là chỗ chứa mấy thứ dùng chung: GSON, `param()`/`jsonBody()` để đọc input từ query hoặc JSON body, `ok()`/`fail()` để chuẩn hoá response, `getAuthenticatedUser()` verify cookie, `hasRole()` check quyền.

Cookie session ký bằng HMAC-SHA256 (`CookieSigner`), so sánh constant-time. SessionId thật nằm bên middleware, cookie chỉ mang `sessionId.signature`.

Upload ảnh (`ImageStorage`) nhận qua `ImageUploadServlet`, check magic bytes chứ không tin extension client gửi lên, lưu tạm vào `uploads/tmp/`, chỉ move sang `uploads/images/<type>/` khi DB lưu thành công, có rollback nếu lưu DB lỗi. Ảnh được Jetty serve tĩnh thẳng từ đĩa qua `/images/*`.

## Cần gì để chạy

Cần JDK 8 trở lên, và `mioto-car-middleware` phải đang chạy sẵn vì service này không tự đứng một mình được. Build bằng `ant` là đủ, không cần mở NetBeans.

## Config

Cũng như middleware, chưa có sẵn `conf/development.ini`, cần tự tạo (log4j.ini thì có sẵn rồi):

```ini
[HttpServer@web]
port=8081
resource=./public

[session@session]
secret=doi-sang-chuoi-bi-mat-dai-khi-len-prod

[mioto@mioto]
uploadDir=uploads
imageBaseUrl=http://127.0.0.1:8081/images
```

`CookieSigner` sẽ fallback về `"change-me"` nếu không set `secret`, nên nhớ đổi trước khi deploy thật - lộ cái này là forge được cookie đăng nhập của người khác.

CORS hiện đang hardcode origin `http://localhost:5173` trong `BaseServlet.setCorsHeader()`, deploy môi trường khác thì phải sửa tay hoặc đưa ra config.

## Chạy

```bash
ant build
java -cp "build/classes:lib/*" -Dappenv=development -Dconf=conf apps.WebDeamon
```

Mặc định port 8081.

## API

Tiền tố `/api`, đăng ký trong `apps/HttpServer.java`:

| Method | Path | Servlet | Auth |
|---|---|---|---|
| * | `/health` | HealthServlet | public |
| POST | `/api/signup` | SignupServlet | public |
| POST | `/api/login` | LoginServlet | public |
| POST | `/api/logout` | LogoutServlet | login |
| GET/POST/PUT | `/api/profiles` | ProfileServlet | login |
| GET | `/api/checkauth` | MeServlet | public |
| CRUD | `/api/cars/*` | CarServlet | GET public, trừ GET cho chủ xe hoặc xe cho admin, ghi cần login |
| CRUD | `/api/car-images/*` | CarImageServlet | GET public, ghi cần login |
| CRUD | `/api/car-features/*` | CarFeatureServlet | GET public, ghi cần login |
| CRUD | `/api/car-unavails/*` | CarUnavailServlet | GET public, ghi cần login + chủ xe |
| CRUD | `/api/car-brands/*` | CarBrandServlet | GET public, ghi cần login |
| CRUD | `/api/districts/*` | DistrictServlet | GET public, ghi cần login |
| CRUD | `/api/provinces/*` | ProvinceServlet | GET public, ghi cần login |
| CRUD | `/api/features/*` | FeatureServlet | GET public, ghi cần login |
| CRUD | `/api/fee-policies/*` | FeePolicyServlet | GET public, ghi cần login |
| CRUD | `/api/feedbacks/*` | FeedBackServlet | GET public, ghi cần login |
| CRUD | `/api/vouchers/*` | VoucherServlet | GET public, ghi cần role VOUCHER_WRITE |
| CRUD | `/api/roles/*` | RoleServlet | login |
| CRUD | `/api/user-roles/*` | UserRoleServlet | login |
| CRUD | `/api/users/*` | UserServlet | login |
| POST | `/api/images` | ImageUploadServlet | login |
| GET | `/images/*` | static, Jetty ResourceHandler | public |
| GET | `/static/*` | static, Jetty ResourceHandler | public |

Response trả về theo kiểu `{ "error": 0, "data": { ... } }` khi thành công, hoặc `{ "error": <mã lỗi>, "message": "<mô tả>" }` khi lỗi.

## Cấu trúc

```
src/
├── apps/      # HttpServer, WebDeamon (entry point)
├── servlet/   # BaseServlet, AuthServlet, AuthWithoutGetServlet + 20+ servlet CRUD
├── util/      # Config, CookieSigner, CookieUtil, RateLimiter, ImageStorage, ClientHolder
├── cache/     # SimpleCache (bản riêng của service này)
├── error/     # Err
├── role/      # Role.VOUCHER_WRITE...
└── imageType/ # ImageType.CAR_TYPE, VOUCHER_TYPE...
```

## Đang biết nhưng chưa sửa

Pattern `if (x == null || x.trim().isEmpty()) { fail(...); return; }` đang lặp lại khoảng 85 lần trên hơn chục servlet, nặng nhất ở `CarServlet.java` với `VoucherServlet.java`. Định gom thành một helper trong `BaseServlet` kiểu `requireString(req, body, field, errorMsg, resp)`, tách riêng bước parse số ra khỏi bước check tồn tại.

Có một bug copy-paste ở `CarUnavailServlet.doPost()` - hai điều kiện validate `startTime`/`endTime` đang check nhầm lại biến `carIdFromParam`. Không làm sai dữ liệu, chỉ khiến message lỗi không đúng khi field truyền vào là chuỗi rỗng thay vì null.

`VoucherServlet.doPut()` bắt phải gửi lại `images` mỗi lần update kể cả khi không đổi ảnh - nên để "không gửi images" nghĩa là giữ nguyên ảnh cũ.
