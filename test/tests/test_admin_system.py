from helpers.constants import LIBRARY_ID


# ── 系统规则 ──────────────────────────────────────────────────────────

def test_get_system_rules_unauthenticated(anon):
    r = anon.get("/admin/system-rules")
    assert r.json()["code"] == "A0100"


def test_get_system_rules_forbidden(user1):
    r = user1.get("/admin/system-rules")
    assert r.json()["code"] == "A0301"


def test_get_system_rules(admin):
    r = admin.get("/admin/system-rules")
    body = r.json()
    assert body["code"] == "00000"
    data = body["data"]
    # 全局默认规则
    assert data["advanceDaysMax"] == 7
    assert data["openTimeStart"] == "07:00:00"
    assert data["openTimeEnd"] == "22:00:00"


# ── 图书馆管理 ────────────────────────────────────────────────────────

def test_list_libraries_forbidden(user1):
    r = user1.get("/admin/libraries")
    assert r.json()["code"] == "A0301"


def test_list_libraries(admin):
    r = admin.get("/admin/libraries")
    body = r.json()
    assert body["code"] == "00000"
    names = [lib["name"] for lib in body["data"]]
    assert "中心图书馆" in names


def test_get_library_by_id(admin):
    r = admin.get(f"/admin/libraries/{LIBRARY_ID}")
    body = r.json()
    assert body["code"] == "00000"
    assert body["data"]["id"] == LIBRARY_ID
    assert body["data"]["name"] == "中心图书馆"


# ── 审计日志 ──────────────────────────────────────────────────────────

def test_list_audit_logs_forbidden(user1):
    r = user1.get("/admin/audit-logs")
    assert r.json()["code"] == "A0301"


def test_list_audit_logs_unauthenticated(anon):
    r = anon.get("/admin/audit-logs")
    assert r.json()["code"] == "A0100"


def test_list_audit_logs(admin):
    r = admin.get("/admin/audit-logs")
    body = r.json()
    assert body["code"] == "00000"
    assert body["data"]["total"] >= 2


def test_list_audit_logs_pagination(admin):
    r = admin.get("/admin/audit-logs", params={"page": 1, "pageSize": 1})
    body = r.json()
    assert body["code"] == "00000"
    assert len(body["data"]["items"]) <= 1
