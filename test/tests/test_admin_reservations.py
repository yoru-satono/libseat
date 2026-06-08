import io
from datetime import date, timedelta

import openpyxl

from helpers.constants import (
    SEAT_AVAIL_Q2, SEAT_AVAIL_Q3,
    RESERVATION_USER1_COMPLETED,
)

TOMORROW = (date.today() + timedelta(days=1)).isoformat()


def _create_payload(seat_id: str, start: str = "10:00", end: str = "11:30") -> dict:
    return {"seatId": seat_id, "date": TOMORROW, "startTime": start, "endTime": end}


# ── 鉴权 ─────────────────────────────────────────────────────────────

def test_get_reservation_forbidden(user1):
    r = user1.get(f"/admin/reservations/{RESERVATION_USER1_COMPLETED}")
    assert r.json()["code"] == "A0301"


def test_get_reservation_unauthenticated(anon):
    r = anon.get(f"/admin/reservations/{RESERVATION_USER1_COMPLETED}")
    assert r.json()["code"] == "A0100"


# ── 单条查询 ──────────────────────────────────────────────────────────

def test_get_reservation_as_admin(admin):
    r = admin.get(f"/admin/reservations/{RESERVATION_USER1_COMPLETED}")
    body = r.json()
    assert body["code"] == "00000"
    data = body["data"]
    # 管理员视图含用户信息
    assert "userNo" in data
    assert "realName" in data
    assert data["status"] == "COMPLETED"


# ── 管理员取消 ────────────────────────────────────────────────────────

def test_admin_cancel_reservation(admin, user1):
    # user1 先创建一条预约
    create = user1.post("/reservations", json=_create_payload(SEAT_AVAIL_Q2, "18:00", "19:30"))
    assert create.json()["code"] == "00000"
    rid = create.json()["data"]["id"]

    r = admin.delete(f"/admin/reservations/{rid}")
    assert r.json()["code"] == "00000"

    detail = user1.get(f"/reservations/{rid}").json()
    assert detail["data"]["status"] == "CANCELLED"


def test_admin_cancel_forbidden(user1):
    r = user1.delete(f"/admin/reservations/{RESERVATION_USER1_COMPLETED}")
    assert r.json()["code"] == "A0301"


# ── 导出 ──────────────────────────────────────────────────────────────

def test_export_reservations_forbidden(user1):
    r = user1.get("/admin/reservations/export")
    assert r.json()["code"] == "A0301"


def test_export_reservations_xlsx(admin):
    r = admin.get("/admin/reservations/export")
    assert r.status_code == 200
    ct = r.headers.get("content-type", "")
    assert "spreadsheetml" in ct or "octet-stream" in ct

    wb = openpyxl.load_workbook(io.BytesIO(r.content))
    sheet = wb.active
    headers = [sheet.cell(1, col).value for col in range(1, 5)]
    assert "序号" in headers
    assert "学号/工号" in headers
    assert "姓名" in headers
