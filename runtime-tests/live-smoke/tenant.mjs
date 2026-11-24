import assert from "node:assert/strict";
import { LiveSession, assertPage } from "./helpers.mjs";

const session = new LiveSession();
const password = process.env.DEMO_TENANT_PASSWORD || "Tenant@123456";
await session.login("nguoithue", password, "/tenant/dashboard");

await assertPage(session, "/tenant/dashboard", 200, "Dashboard Người thuê");

// Kiểm tra toàn bộ link trong navbar của Tenant
const tenantNavbarLinks = [
  "/",
  "/rooms/list",
  "/ranking",
  "/tenant/dashboard",
  "/tenant/reviews",
  "/reviews"
];
for (const path of tenantNavbarLinks) {
  const res = await session.request(path);
  assert.ok(res.status === 200 || res.status === 302, `Tenant navbar link ${path} should return 200 or 302, got ${res.status}`);
}

const roomsPage = await assertPage(session, "/rooms/list", 200, "Tìm Kiếm Phòng Trọ");
assert.ok(roomsPage.includes("14 kết quả"), "/rooms/list should render 14 kết quả");

await assertPage(session, "/rooms/3", 200, "Phòng trọ giá sinh viên");
await assertPage(session, "/admin/dashboard", 403, "không có quyền");
await assertPage(session, "/landlord/dashboard", 403, "không có quyền");

const absentBooking = await session.request("/booking-requests");
assert.ok(absentBooking.status === 403 || absentBooking.status === 404, "/booking-requests should be rejected with 403 or 404");

const malformedFilter = await session.request("/rooms/list?floorNo=not-a-number");
assert.equal(malformedFilter.status, 400, "invalid filter value should be rejected");

await session.logout();
const afterLogout = await session.request("/tenant/dashboard");
assert.equal(afterLogout.status, 302);
assert.equal(new URL(afterLogout.headers.get("location"), session.baseUrl).pathname, "/login");
console.log("PASS tenant login, navbar scan, /rooms/list 14 rooms, /reviews 200/302, role denial, logout/session invalidation");
