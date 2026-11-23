import assert from "node:assert/strict";
import { LiveSession, assertPage } from "./helpers.mjs";

const session = new LiveSession();
assert.ok(process.env.DEMO_ADMIN_PASSWORD, "set DEMO_ADMIN_PASSWORD to run this live check");
await session.login("admin", process.env.DEMO_ADMIN_PASSWORD, "/admin/dashboard");

const dashboard = await assertPage(session, "/admin/dashboard", 200, "Tổng quan hệ thống");
assert.ok(dashboard.includes("Tổng tài khoản"));
assert.ok(dashboard.includes("14"), "dashboard should show the 14 public listings read from the database");

for (const path of [
  "/admin/accounts/landlords",
  "/admin/accounts/tenants",
  "/admin/landlords/registrations",
  "/admin/rooms",
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
console.log("PASS admin login, database dashboard, read-only admin views, role denial, logout/session invalidation");
