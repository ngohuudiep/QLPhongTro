import assert from "node:assert/strict";
import { LiveSession } from "./live-smoke/helpers.mjs";

function getCsrf(html) {
  const match = html.match(/name="_csrf"\s+value="([^"]+)"/i);
  return match ? match[1] : null;
}

let lastCsrfTokens = new Map();

async function getPageCsrf(session, path) {
  const page = await session.request(path);
  const text = await page.text();
  const token = getCsrf(text);
  if (token) {
    lastCsrfTokens.set(session, token);
  }
  return { page, text, token: token || lastCsrfTokens.get(session) || "" };
}

const adminPass = process.env.DEMO_ADMIN_PASSWORD || "Admin@123456";
const landlordPass = process.env.DEMO_LANDLORD_PASSWORD || "Landlord@123456";
const tenantPass = process.env.DEMO_TENANT_PASSWORD || "Tenant@123456";

console.log("=== STARTING FULL BUSINESS FLOW VERIFICATION ===");

const tenantSession = new LiveSession();
await tenantSession.login("nguoithue", tenantPass, "/tenant/dashboard");

const landlordSession = new LiveSession();
await landlordSession.login("chutro", landlordPass, "/landlord/dashboard");

// ====================================================================
// LUỒNG A: ĐẶT PHÒNG -> TẠO HỢP ĐỒNG -> KHÁCH DUYỆT -> XÁC NHẬN CỌC
// ====================================================================
console.log("\n--- LUỒNG A: Đặt phòng -> Hợp đồng -> Xác nhận cọc ---");

// Kiểm tra xem đã có contract ACTIVE hay cần tạo mới
let contractId = null;
const { text: landlordContractsHtml } = await getPageCsrf(landlordSession, "/landlord/contracts");
const activeContractMatch = landlordContractsHtml.match(/\/landlord\/contracts\/(\d+)[\s\S]{1,500}?ACTIVE/i) ||
  landlordContractsHtml.match(/ACTIVE[\s\S]{1,500}?\/landlord\/contracts\/(\d+)/i);

// Kiểm tra có deposit request đang chờ duyệt không
const { text: initDepositReqHtml, token: initDepCsrf } = await getPageCsrf(landlordSession, "/landlord/rental/deposit-requests");
const initDepMatch = initDepositReqHtml.match(/\/landlord\/rental\/deposit-requests\/(\d+)\/approve/);

if (initDepMatch) {
  const depReqId = initDepMatch[1];
  console.log(`Duyệt yêu cầu cọc đang chờ #${depReqId}...`);
  const approveDepositRes = await landlordSession.request(`/landlord/rental/deposit-requests/${depReqId}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ referenceNo: "VNPAY-DEP-001", _csrf: initDepCsrf })
  });
  assert.equal(approveDepositRes.status, 302, "Approve deposit should redirect");
  console.log(`✓ Chủ trọ đã duyệt tiền cọc #${depReqId}`);
}

if (activeContractMatch) {
  contractId = activeContractMatch[1];
  console.log(`✓ Hợp đồng #${contractId} đã sẵn sàng ở trạng thái ACTIVE`);
} else {
  // 1. Tenant gửi yêu cầu thuê phòng P101 (listingId = 1)
  const { token: reqCsrf } = await getPageCsrf(tenantSession, "/rooms/list");
  const createReqRes = await tenantSession.request("/tenant/rental/request", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      listingId: "1",
      expectedOccupants: "1",
      expectedMoveInDate: "2026-12-01",
      note: "Khách thuê công tác dài hạn 1 năm",
      _csrf: reqCsrf
    })
  });
  assert.equal(createReqRes.status, 302, "Create rental request should redirect");
  console.log("✓ Tenant đã gửi yêu cầu thuê phòng thành công");

  // 2. Chủ trọ duyệt yêu cầu thuê
  const { text: requestsHtml } = await getPageCsrf(landlordSession, "/landlord/rental/requests");
  const requestIdMatch = requestsHtml.match(/\/landlord\/rental\/requests\/(\d+)/);
  assert.ok(requestIdMatch, "Landlord should see the rental request");
  const requestId = requestIdMatch[1];

  const { token: detailCsrf } = await getPageCsrf(landlordSession, `/landlord/rental/requests/${requestId}`);
  const approveReqRes = await landlordSession.request(`/landlord/rental/requests/${requestId}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ _csrf: detailCsrf })
  });
  assert.equal(approveReqRes.status, 302, "Approve request should redirect to contract edit");
  const editLocation = approveReqRes.headers.get("location");
  const contractIdMatch = editLocation ? editLocation.match(/\/contracts\/(\d+)\/edit/) : null;
  assert.ok(contractIdMatch, `Should redirect to contract edit page but was: ${editLocation}`);
  contractId = contractIdMatch[1];
  console.log(`✓ Chủ trọ duyệt yêu cầu #${requestId}, tạo bản nháp hợp đồng #${contractId}`);

  // Gửi hợp đồng cho khách thuê
  const { token: editCsrf } = await getPageCsrf(landlordSession, `/landlord/rental/contracts/${contractId}/edit`);
  const sendContractRes = await landlordSession.request(`/landlord/rental/contracts/${contractId}/send`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      contractCode: `HD-P101-${Date.now().toString().slice(-4)}`,
      startDate: "2026-12-01",
      endDate: "2027-12-01",
      rentPrice: "3800000",
      depositAmount: "3800000",
      _csrf: editCsrf
    })
  });
  assert.equal(sendContractRes.status, 302, "Send contract should redirect");
  console.log(`✓ Chủ trọ đã gửi hợp đồng #${contractId} cho người thuê`);

  // 3. Khách thuê đồng ý hợp đồng và gửi yêu cầu xác nhận tiền cọc
  const { token: tenantContractCsrf } = await getPageCsrf(tenantSession, `/tenant/rental/contracts/${contractId}`);
  const acceptRes = await tenantSession.request(`/tenant/rental/contracts/${contractId}/accept`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ _csrf: tenantContractCsrf })
  });
  assert.equal(acceptRes.status, 302, "Tenant accept should redirect");
  console.log(`✓ Khách thuê đã ký chấp nhận hợp đồng #${contractId}`);

  const depositRes = await tenantSession.request(`/tenant/rental/contracts/${contractId}/deposit`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ _csrf: tenantContractCsrf })
  });
  assert.equal(depositRes.status, 302, "Tenant deposit request should redirect");
  console.log(`✓ Khách thuê đã gửi yêu cầu xác nhận tiền cọc cho hợp đồng #${contractId}`);

  // 4. Chủ trọ xác nhận tiền cọc
  const { text: depositRequestsHtml, token: depCsrf } = await getPageCsrf(landlordSession, "/landlord/rental/deposit-requests");
  const depositReqIdMatch = depositRequestsHtml.match(/\/landlord\/rental\/deposit-requests\/(\d+)\/approve/);
  assert.ok(depositReqIdMatch, "Landlord should see deposit confirmation request");
  const depositReqId = depositReqIdMatch[1];

  const approveDepositRes = await landlordSession.request(`/landlord/rental/deposit-requests/${depositReqId}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ referenceNo: "VNPAY-DEP-001", _csrf: depCsrf })
  });
  assert.equal(approveDepositRes.status, 302, "Approve deposit should redirect");
  console.log(`✓ Chủ trọ đã duyệt tiền cọc #${depositReqId} -> Hợp đồng #${contractId} chính thức có hiệu lực (ACTIVE)`);
}

// ====================================================================
// LUỒNG B: ĐIỆN NƯỚC -> HÓA ĐƠN -> THANH TOÁN
// ====================================================================
console.log("\n--- LUỒNG B: Ghi chỉ số -> Lập hóa đơn -> Thanh toán ---");

// Ghi chỉ số điện nước
const { token: meterCsrf } = await getPageCsrf(landlordSession, "/meter-readings");
const meterRes = await landlordSession.request("/meter-readings/save", {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    "room.roomId": "1",
    readDate: "2026-12-01",
    startIndex: "100",
    endIndex: "250",
    _csrf: meterCsrf
  })
});
assert.equal(meterRes.status, 302, "Save meter reading should redirect");
console.log("✓ Chủ trọ đã chốt chỉ số điện nước thành công");

// Lập hóa đơn chi tiết tháng 12/2026 nếu chưa có
const { text: initInvoicesHtml } = await getPageCsrf(landlordSession, "/landlord/invoices");
let invoiceIdMatch = initInvoicesHtml.match(/\/landlord\/invoices\/(\d+)/);

if (!invoiceIdMatch) {
  const { token: invoicePageCsrf } = await getPageCsrf(landlordSession, "/landlord/invoices");
  const invoiceRes = await landlordSession.request("/landlord/invoices", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      contractId: contractId,
      month: "2026-12",
      dueDate: "2026-12-25",
      roomAmount: "3800000",
      electricityPreviousReading: "100",
      electricityCurrentReading: "250",
      electricityUnitPrice: "3500",
      waterPreviousReading: "10",
      waterCurrentReading: "25",
      waterUnitPrice: "25000",
      notes: "Hóa đơn tiền phòng & tiện ích tháng 12/2026",
      _csrf: invoicePageCsrf
    })
  });
  assert.equal(invoiceRes.status, 302, "Create invoice should redirect");
  console.log(`✓ Đã tạo hóa đơn chi tiết tháng 12/2026 cho hợp đồng #${contractId}`);

  const { text: updatedInvoicesHtml } = await getPageCsrf(landlordSession, "/landlord/invoices");
  invoiceIdMatch = updatedInvoicesHtml.match(/\/landlord\/invoices\/(\d+)/);
}
assert.ok(invoiceIdMatch, "Should find invoice in landlord invoices list");
const invoiceId = invoiceIdMatch[1];

// Kiểm tra hóa đơn và xác nhận thanh toán nếu chưa thanh toán
const { text: invDetailHtml, token: invoiceDetailCsrf } = await getPageCsrf(landlordSession, `/landlord/invoices/${invoiceId}`);
if (invDetailHtml.includes("Ghi nhận tiền đã nhận")) {
  const payRes = await landlordSession.request(`/landlord/invoices/${invoiceId}/confirm-paid`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      method: "BANK_TRANSFER",
      referenceNo: `VNPAY-${Date.now().toString().slice(-6)}`,
      _csrf: invoiceDetailCsrf
    })
  });
  assert.equal(payRes.status, 302, "Confirm pay invoice should redirect");
  console.log(`✓ Đã ghi nhận thanh toán thành công cho hóa đơn #${invoiceId}`);
} else {
  console.log(`✓ Hóa đơn #${invoiceId} đã được ghi nhận thanh toán thành công (PAID)`);
}

// ====================================================================
// LUỒNG D (Chạy trước khi kết thúc hợp đồng để dùng contractId ACTIVE)
// ====================================================================
console.log("\n--- LUỒNG D: Sự cố Tenant, Admin duyệt đăng ký chủ trọ & Khóa tài khoản ---");

// 1. Tenant gửi báo cáo sự cố có đầy đủ tiêu đề và mô tả tiếng Việt
const { token: incCsrf } = await getPageCsrf(tenantSession, "/tenant/incidents");
const incidentRes = await tenantSession.request("/tenant/incidents", {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    contractId: contractId,
    title: "Đèn hành lang bị cháy",
    description: "Bóng đèn hành lang trước cửa phòng chập chờn rồi tắt hẳn vào tối qua.",
    priority: "MEDIUM",
    _csrf: incCsrf
  })
});
assert.equal(incidentRes.status, 302, "Incident report should redirect");
console.log("✓ Khách thuê đã gửi phản ánh sự cố kèm mô tả chi tiết thành công");

// 2. Đăng ký chủ trọ mới và Admin phê duyệt
const publicSession = new LiveSession();
const { token: regCsrf } = await getPageCsrf(publicSession, "/register");
const newLandlordUser = `chutro_${Date.now().toString().slice(-4)}`;
const regRes = await publicSession.request("/register", {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    fullName: "Chủ Trọ Mới Thử Nghiệm",
    username: newLandlordUser,
    email: `${newLandlordUser}@test.vn`,
    phone: `091${Date.now().toString().slice(-7)}`,
    password: "Password123!",
    confirmPassword: "Password123!",
    roleChoice: "LANDLORD",
    _csrf: regCsrf
  })
});
assert.equal(regRes.status, 302, "New landlord registration should redirect to login");
console.log(`✓ Người dùng đã đăng ký tài khoản chủ trọ mới: ${newLandlordUser} (ở trạng thái chờ duyệt)`);

// Admin đăng nhập để duyệt
const adminSession = new LiveSession();
await adminSession.login("admin", adminPass, "/admin/dashboard");

const { text: regListHtml, token: adminRegCsrf } = await getPageCsrf(adminSession, "/admin/landlords/registrations");
const approveMatch = regListHtml.match(new RegExp(`/admin/landlords/registrations/(\\d+)`));
assert.ok(approveMatch, "Admin should see pending landlord registration");
const newLandlordId = approveMatch[1];

const approveLandlordRes = await adminSession.request(`/admin/landlords/registrations/${newLandlordId}/approve`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({ _csrf: adminRegCsrf })
});
assert.equal(approveLandlordRes.status, 302, "Admin approve landlord should redirect");
console.log(`✓ Admin đã duyệt thành công tài khoản chủ trọ #${newLandlordId} (${newLandlordUser})`);

// 3. Admin khóa và mở khóa tài khoản
const { token: adminAccountCsrf } = await getPageCsrf(adminSession, "/admin/accounts/tenants");
const lockRes = await adminSession.request("/admin/accounts/tenants/3/enabled", {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    enabled: "false",
    _csrf: adminAccountCsrf
  })
});
assert.equal(lockRes.status, 302, "Lock account should redirect");
console.log("✓ Admin đã khóa tài khoản người thuê #3 (nguoithue)");

const unlockRes = await adminSession.request("/admin/accounts/tenants/3/enabled", {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    enabled: "true",
    _csrf: adminAccountCsrf
  })
});
assert.equal(unlockRes.status, 302, "Unlock account should redirect");
console.log("✓ Admin đã mở khóa tài khoản người thuê #3 (nguoithue)");

// ====================================================================
// LUỒNG C: YÊU CẦU KẾT THÚC HỢP ĐỒNG -> QUYẾT TOÁN HOÀN CỌC
// ====================================================================
console.log("\n--- LUỒNG C: Khách yêu cầu kết thúc -> Quyết toán hoàn cọc ---");

// Sau khi Admin khóa và mở khóa tài khoản, tạo phiên người thuê mới
const retenantSession = new LiveSession();
await retenantSession.login("nguoithue", tenantPass, "/tenant/dashboard");

// Khách yêu cầu chấm dứt hợp đồng
const { token: termCsrf } = await getPageCsrf(retenantSession, "/tenant/contracts");
const termReqRes = await retenantSession.request(`/tenant/contracts/${contractId}/termination-requests`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    reasonCode: "Chuyển nơi ở",
    reasonDetails: "Chuyển địa điểm làm việc sang quận khác",
    expectedTerminationDate: "2026-12-31",
    notes: "Xin bàn giao phòng vào cuối tháng 12",
    _csrf: termCsrf
  })
});
assert.equal(termReqRes.status, 302, "Termination request should redirect");
const termLocation = termReqRes.headers.get("location");
const termIdMatch = termLocation.match(/\/tenant\/terminations\/(\d+)/);
assert.ok(termIdMatch, `Should redirect to termination details but was: ${termLocation}`);
const termId = termIdMatch[1];
console.log(`✓ Khách thuê đã gửi yêu cầu kết thúc hợp đồng #${contractId} (yêu cầu #${termId})`);

// Chủ trọ đồng ý yêu cầu kết thúc
const { token: landTermCsrf } = await getPageCsrf(landlordSession, `/landlord/terminations/${termId}`);
const landlordAcceptRes = await landlordSession.request(`/landlord/terminations/${termId}/respond`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    accept: "true",
    reason: "Đồng ý hỗ trợ khách chuyển nơi làm việc",
    _csrf: landTermCsrf
  })
});
assert.equal(landlordAcceptRes.status, 302, "Landlord respond should redirect");
console.log(`✓ Chủ trọ đã phê duyệt yêu cầu kết thúc #${termId}`);

// Chủ trọ lập quyết toán thanh lý
const { token: settleCsrf } = await getPageCsrf(landlordSession, `/landlord/terminations/${termId}`);
const settlementRes = await landlordSession.request(`/landlord/terminations/${termId}/settlement`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    depositAmount: "3800000",
    rentThroughMoveOutAmount: "0",
    electricityOldReading: "100",
    electricityNewReading: "150",
    electricityUnitPrice: "3500",
    waterOldReading: "10",
    waterNewReading: "15",
    waterUnitPrice: "25000",
    serviceFeeAmount: "0",
    compensationAmount: "0",
    depositDeductionAmount: "300000",
    depositDeductionReason: "Khấu trừ phí dọn dẹp vệ sinh bàn giao",
    roomHandoverCondition: "Bàn giao phòng sạch sẽ, đầy đủ chìa khóa và thiết bị",
    _csrf: settleCsrf
  })
});
assert.equal(settlementRes.status, 302, "Settlement submission should redirect");
console.log(`✓ Chủ trọ đã lập bảng quyết toán hoàn cọc #${termId}`);

// Khách thuê đồng ý quyết toán
const { token: tenantSettleCsrf } = await getPageCsrf(retenantSession, `/tenant/terminations/${termId}`);
const confirmSettlementRes = await retenantSession.request(`/tenant/terminations/${termId}/settlement/confirm`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({ _csrf: tenantSettleCsrf })
});
assert.equal(confirmSettlementRes.status, 302, "Tenant confirm settlement should redirect");
console.log(`✓ Khách thuê đã xác nhận đồng ý quyết toán #${termId}`);

// Chủ trọ xác nhận chuyển tiền hoàn cọc thực tế
const { token: transCsrf } = await getPageCsrf(landlordSession, `/landlord/terminations/${termId}`);
const transferRes = await landlordSession.request(`/landlord/terminations/${termId}/settlement/transfers`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    tenantPaymentReference: "PAY-TRANSFER-20261231-01",
    depositRefundReference: "REFUND-VNPAY-20261231-01",
    _csrf: transCsrf
  })
});
assert.equal(transferRes.status, 302, "Confirm transfers should redirect");
console.log(`✓ Chủ trọ đã xác nhận hoàn tiền cọc thực tế (mã giao dịch: REFUND-VNPAY-20261231-01)`);

// Chủ trọ hoàn tất kết thúc hợp đồng
const { token: completeCsrf } = await getPageCsrf(landlordSession, `/landlord/terminations/${termId}`);
const completeTermRes = await landlordSession.request(`/landlord/terminations/${termId}/complete`, {
  method: "POST",
  headers: { "Content-Type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({ _csrf: completeCsrf })
});
assert.equal(completeTermRes.status, 302, "Complete termination should redirect");
console.log(`✓ Chủ trọ đã hoàn tất quyết toán và đóng hợp đồng #${contractId} thành công`);

try { await retenantSession.logout(); } catch (_) {}
try { await landlordSession.logout(); } catch (_) {}
try { await adminSession.logout(); } catch (_) {}

console.log("\n=======================================================");
console.log("TẤT CẢ CÁC LUỒNG NGHIỆP VỤ A, B, C, D ĐÃ CHẠY THÀNH CÔNG 100%!");
console.log("=======================================================");
