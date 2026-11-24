import assert from "node:assert/strict";
import { LiveSession, assertPage } from "./helpers.mjs";

const session = new LiveSession();
const password = process.env.DEMO_ADMIN_PASSWORD || "Admin@123456";
await session.login("admin", password, "/admin/dashboard");

const dashboard = await assertPage(session, "/admin/dashboard", 200, "Tổng quan hệ thống");
assert.ok(dashboard.includes("Tổng tài khoản"));
assert.ok(dashboard.includes("14"), "dashboard should show the 14 public listings read from the database");

// Kiểm tra toàn bộ link trong navbar của Admin
const adminNavbarLinks = [
  "/",
  "/rooms/list",
  "/ranking",
  "/admin/dashboard",
  "/admin/reviews",
  "/admin/rooms",
  "/admin/staff-assignments",
  "/reviews"
];
for (const path of adminNavbarLinks) {
  const res = await session.request(path);
  assert.ok(res.status === 200 || res.status === 302, `Admin navbar link ${path} should return 200 or 302, got ${res.status}`);
}

// Kiểm tra /rooms/list trả về đủ 14 phòng
const roomsPage = await session.request("/rooms/list");
assert.equal(roomsPage.status, 200);
const roomsHtml = await roomsPage.text();
assert.ok(roomsHtml.includes("14 kết quả"), "/rooms/list should render 14 kết quả");

for (const path of [
  "/admin/accounts/landlords",
  "/admin/accounts/tenants",
  "/admin/landlords/registrations",
  "/admin/buildings",
  "/admin/managed-rooms"
]) {
  await assertPage(session, path, 200);
}

await assertPage(session, "/landlord/dashboard", 403, "không có quyền");
await session.logout();
const afterLogout = await session.request("/admin/dashboard");
assert.equal(afterLogout.status, 302);
assert.equal(new URL(afterLogout.headers.get("location"), session.baseUrl).pathname, "/login");
console.log("PASS admin login, navbar scan, /rooms/list 14 rooms, /reviews 200/302, role denial, logout/session invalidation");
