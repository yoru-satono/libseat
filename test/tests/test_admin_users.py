from helpers.constants import USER1_ID, USER2_ID, CHANGE_REQUEST_PENDING


# ── 鉴权 ─────────────────────────────────────────────────────────────

def test_list_users_unauthenticated(anon):
    r = anon.get("/admin/users")
    assert r.json()["code"] == "A0100"


def test_list_users_regular_user_forbidden(user1):
    r = user1.get("/admin/users")
    assert r.json()["code"] == "A0301"


# ── 用户列表 ──────────────────────────────────────────────────────────

def test_list_users_as_admin(admin):
    r = admin.get("/admin/users")
    body = r.json()
    assert body["code"] == "00000"
    assert body["data"]["total"] >= 8


def test_list_users_filter_by_role(admin):
    r = admin.get("/admin/users", params={"role": "STUDENT"})
    body = r.json()
    assert body["code"] == "00000"
    for user in body["data"]["items"]:
        assert user["role"] == "STUDENT"


def test_list_users_filter_by_status(admin):
    r = admin.get("/admin/users", params={"status": "ACTIVE"})
    body = r.json()
    assert body["code"] == "00000"
    for user in body["data"]["items"]:
        assert user["status"] == "ACTIVE"


def test_list_users_pagination(admin):
    r = admin.get("/admin/users", params={"page": 1, "pageSize": 3})
    body = r.json()
    assert body["code"] == "00000"
    assert len(body["data"]["items"]) <= 3


# ── 变更申请管理 ───────────────────────────────────────────────────────

def test_list_change_requests_as_admin(admin):
    r = admin.get("/admin/change-requests")
    body = r.json()
    assert body["code"] == "00000"
    # 列表返回待处理申请，种子申请可能已被上轮测试审批
    assert "items" in body["data"]


def test_list_change_requests_forbidden(user1):
    r = user1.get("/admin/change-requests")
    assert r.json()["code"] == "A0301"


def test_approve_change_request(admin, user2):
    # user2 先提交一条变更申请
    create = user2.post("/users/me/change-requests",
                        json={"fieldName": "department", "newValue": "测试学院"})
    assert create.json()["code"] == "00000"
    rid = create.json()["data"]["id"]

    r = admin.patch(f"/admin/change-requests/{rid}",
                    json={"action": "APPROVED", "handleNote": "证件核实通过"})
    assert r.json()["code"] == "00000"
