import assert from "node:assert/strict";
import { LiveSession, assertPage } from "./helpers.mjs";

const session = new LiveSession();
assert.ok(process.env.DEMO_TENANT_PASSWORD, "set DEMO_TENANT_PASSWORD to run this live check");
await session.login("nguoithue", process.env.DEMO_TENANT_PASSWORD, "/tenant/dashboard");

await assertPage(session, "/tenant/dashboard", 200, "Dashboard Người thuê");
await assertPage(session, "/rooms/list", 200, "Tìm Kiếm Phòng Trọ");
await assertPage(session, "/rooms/3", 200, "Phòng trọ giá sinh viên");
await assertPage(session, "/admin/dashboard", 403, "không có quyền");
await assertPage(session, "/landlord/dashboard", 403, "không có quyền");
await assertPage(session, "/booking-requests", 404, "Not Found");

const malformedFilter = await session.request("/rooms/list?floorNo=not-a-number");
assert.equal(malformedFilter.status, 400, "invalid filter value should be rejected");

await session.logout();
const afterLogout = await session.request("/tenant/dashboard");
assert.equal(afterLogout.status, 302);
assert.equal(new URL(afterLogout.headers.get("location"), session.baseUrl).pathname, "/login");
console.log("PASS tenant login, public room read, role denial, absent booking route, validation response, logout/session invalidation");
