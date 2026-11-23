import assert from "node:assert/strict";
import { LiveSession, assertPage } from "./helpers.mjs";

const session = new LiveSession();
assert.ok(process.env.DEMO_LANDLORD_PASSWORD, "set DEMO_LANDLORD_PASSWORD to run this live check");
await session.login("chutro", process.env.DEMO_LANDLORD_PASSWORD, "/landlord/dashboard");

const dashboard = await assertPage(session, "/landlord/dashboard", 200, "Xin chào,");
assert.ok(dashboard.includes("<span>chutro</span>"));
assert.ok(dashboard.includes("Tổng phòng"));
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
console.log("PASS landlord login, read-only workspace views, admin denial, logout/session invalidation");
