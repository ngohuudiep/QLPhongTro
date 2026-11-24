import assert from "node:assert/strict";

export class LiveSession {
  constructor(baseUrl = "http://localhost:8080") {
    this.baseUrl = baseUrl;
    this.cookies = new Map();
  }

  async request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (this.cookies.size) {
      headers.set("Cookie", [...this.cookies].map(([name, value]) => `${name}=${value}`).join("; "));
    }

    const response = await fetch(new URL(path, this.baseUrl), {
      ...options,
      headers,
      redirect: "manual"
    });

    for (const cookie of response.headers.getSetCookie()) {
      const [pair, ...attributes] = cookie.split(";");
      const separator = pair.indexOf("=");
      if (separator < 0) continue;
      const name = pair.slice(0, separator).trim();
      const value = pair.slice(separator + 1).trim();
      if (attributes.some(attribute => /^\s*max-age=0\s*$/i.test(attribute))) {
        this.cookies.delete(name);
      } else {
        this.cookies.set(name, value);
      }
    }
    return response;
  }

  async login(username, password, expectedLocation) {
    const loginPage = await this.request("/login");
    assert.equal(loginPage.status, 200, "login form should render");
    const token = findInputValue(await loginPage.text(), "_csrf");
    assert.ok(token, "login form should contain a CSRF token");

    const response = await this.request("/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username, password, _csrf: token })
    });
    assert.equal(response.status, 302, "successful form login should redirect");
    assert.equal(new URL(response.headers.get("location"), this.baseUrl).pathname, expectedLocation);
  }

  async logout() {
    const page = await this.request("/login");
    const token = findInputValue(await page.text(), "_csrf");
    assert.ok(token, "page should contain a CSRF token for logout");

    const response = await this.request("/logout", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ _csrf: token })
    });
    assert.equal(response.status, 302, "logout should redirect after invalidating the session");
    const location = new URL(response.headers.get("location"), this.baseUrl);
    assert.equal(location.pathname + location.search, "/login?logout=true");
  }
}

export async function assertPage(session, path, expectedStatus, expectedText) {
  const response = await session.request(path);
  assert.equal(response.status, expectedStatus, `${path} status`);
  const body = await response.text();
  if (expectedText) assert.ok(body.includes(expectedText), `${path} should contain "${expectedText}"`);
  return body;
}

function findInputValue(html, name) {
  const inputs = html.match(/<input\b[^>]*>/gi) || [];
  for (const input of inputs) {
    const nameMatch = input.match(/\bname=["']([^"']+)["']/i);
    if (nameMatch?.[1] !== name) continue;
    return input.match(/\bvalue=["']([^"']*)["']/i)?.[1] || "";
  }
  return null;
}
