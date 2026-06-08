from datetime import date, timedelta

from helpers.constants import (
    SEAT_AVAIL_Q1, SEAT_AVAIL_Q2, SEAT_AVAIL_Q3, SEAT_AVAIL_Q4, SEAT_AVAIL_Q5,
    RESERVATION_USER1_COMPLETED,
)

# 动态日期：明天（保证是未来且在预约窗口内）
TOMORROW = (date.today() + timedelta(days=1)).isoformat()


def _create_payload(seat_id: str, start: str = "10:00", end: str = "11:30") -> dict:
    return {"seatId": seat_id, "date": TOMORROW, "startTime": start, "endTime": end}


# ── 创建 ──────────────────────────────────────────────────────────────

def test_create_reservation_success(user1):
    r = user1.post("/reservations", json=_create_payload(SEAT_AVAIL_Q3))
    body = r.json()
    assert body["code"] == "00000"
    assert body["data"]["status"] == "ACTIVE"
    # 清理：取消刚创建的预约
    rid = body["data"]["id"]
    user1.delete(f"/reservations/{rid}")


def test_create_reservation_unauthenticated(anon):
    r = anon.post("/reservations", json=_create_payload(SEAT_AVAIL_Q4))
    assert r.json()["code"] == "A0100"


def test_create_reservation_missing_fields(user1):
    r = user1.post("/reservations", json={"seatId": SEAT_AVAIL_Q4})
    assert r.json()["code"] == "A0400"


def test_create_duplicate_seat_time_conflict(user1, user2):
    # user1 先占座
    r1 = user1.post("/reservations", json=_create_payload(SEAT_AVAIL_Q5, "13:00", "14:30"))
    assert r1.json()["code"] == "00000"
    rid = r1.json()["data"]["id"]

    # user2 同一座位同时段，应冲突
    r2 = user2.post("/reservations", json=_create_payload(SEAT_AVAIL_Q5, "13:00", "14:30"))
    assert r2.json()["code"] == "B0200"

    # 清理
    user1.delete(f"/reservations/{rid}")


# ── 查询 ──────────────────────────────────────────────────────────────

def test_list_my_reservations(user1):
    r = user1.get("/reservations")
    body = r.json()
    assert body["code"] == "00000"
    for key in ("items", "total", "page", "pageSize", "totalPages"):
        assert key in body["data"]


def test_list_my_reservations_unauthenticated(anon):
    r = anon.get("/reservations")
    assert r.json()["code"] == "A0100"


def test_filter_by_status_completed(user1):
    r = user1.get("/reservations", params={"status": "COMPLETED"})
    body = r.json()
    assert body["code"] == "00000"
    for item in body["data"]["items"]:
        assert item["status"] == "COMPLETED"


def test_get_reservation_by_id(user1):
    r = user1.get(f"/reservations/{RESERVATION_USER1_COMPLETED}")
    body = r.json()
    assert body["code"] == "00000"
    assert body["data"]["status"] == "COMPLETED"


def test_get_other_users_reservation_forbidden(user2):
    # user2 cannot read user1's reservation
    r = user2.get(f"/reservations/{RESERVATION_USER1_COMPLETED}")
    assert r.json()["code"] in ("A0301", "A0302", "B0100")


# ── 取消 ──────────────────────────────────────────────────────────────

def test_cancel_reservation(user1):
    create = user1.post("/reservations", json=_create_payload(SEAT_AVAIL_Q4, "15:00", "16:30"))
    assert create.json()["code"] == "00000"
    rid = create.json()["data"]["id"]

    r = user1.delete(f"/reservations/{rid}")
    assert r.json()["code"] == "00000"


def test_cancel_with_reason(user1):
    create = user1.post("/reservations", json=_create_payload(SEAT_AVAIL_Q4, "16:00", "17:30"))
    assert create.json()["code"] == "00000"
    rid = create.json()["data"]["id"]

    r = user1.request("DELETE", f"/reservations/{rid}", json={"cancelReason": "临时有事"})
    assert r.json()["code"] == "00000"


def test_cancel_already_cancelled(user1):
    create = user1.post("/reservations", json=_create_payload(SEAT_AVAIL_Q3, "17:00", "18:30"))
    rid = create.json()["data"]["id"]
    user1.delete(f"/reservations/{rid}")

    r = user1.delete(f"/reservations/{rid}")
    assert r.json()["code"] != "00000"


# ── 导出 ──────────────────────────────────────────────────────────────

def test_export_reservations_unauthenticated(anon):
    r = anon.get("/reservations/export")
    assert r.json()["code"] == "A0100"


def test_export_reservations_xlsx(user1):
    r = user1.get("/reservations/export")
    assert r.status_code == 200
    ct = r.headers.get("content-type", "")
    assert "spreadsheetml" in ct or "octet-stream" in ct
    assert "attachment" in r.headers.get("content-disposition", "").lower()
    assert len(r.content) > 0
