import assert from "node:assert/strict";
import { LiveSession, assertPage } from "./helpers.mjs";

const session = new LiveSession();
const password = process.env.DEMO_LANDLORD_PASSWORD || "Landlord@123456";
await session.login("chutro", password, "/landlord/dashboard");

const dashboard = await assertPage(session, "/landlord/dashboard", 200, "Xin chào,");
assert.ok(dashboard.includes("chutro") || dashboard.includes("Chủ trọ"));
assert.ok(dashboard.includes("Tổng phòng"));

// Kiểm tra toàn bộ link trong navbar của Landlord
const landlordNavbarLinks = [
  "/",
  "/rooms/list",
  "/ranking",
  "/landlord/dashboard",
  "/landlord/reviews",
  "/reviews"
];
for (const path of landlordNavbarLinks) {
  const res = await session.request(path);
  assert.ok(res.status === 200 || res.status === 302, `Landlord navbar link ${path} should return 200 or 302, got ${res.status}`);
}

for (const path of [
  "/landlord/buildings",
  "/landlord/rooms",
  "/landlord/tenants",
  "/landlord/contracts",
  "/landlord/invoices"
]) {
  await assertPage(session, path, 200);
}

await assertPage(session, "/admin/dashboard", 403, "không có quyền");
await session.logout();
const afterLogout = await session.request("/landlord/dashboard");
assert.equal(afterLogout.status, 302);
assert.equal(new URL(afterLogout.headers.get("location"), session.baseUrl).pathname, "/login");
console.log("PASS landlord login, navbar scan, /reviews 200/302, workspace views, admin denial, logout/session invalidation");
