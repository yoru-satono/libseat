import pytest

from helpers.constants import USER1_NO, CHANGE_REQUEST_PENDING


def test_get_me_unauthenticated(anon):
    r = anon.get("/users/me")
    assert r.json()["code"] == "A0100"


def test_get_me_returns_profile(user1):
    r = user1.get("/users/me")
    body = r.json()
    assert body["code"] == "00000"
    assert body["data"]["userNo"] == USER1_NO


def test_get_me_fields_complete(user1):
    data = user1.get("/users/me").json()["data"]
    for field in ("id", "userNo", "realName", "email", "role", "status"):
        assert field in data, f"missing field: {field}"


def test_update_profile_phone(user1):
    r = user1.patch("/users/me", json={"phone": "13900000099"})
    assert r.json()["code"] == "00000"
    # 还原
    user1.patch("/users/me", json={"phone": "13800001001"})


def test_update_profile_unauthenticated(anon):
    r = anon.patch("/users/me", json={"phone": "13900000099"})
    assert r.json()["code"] == "A0100"


def test_list_change_requests(user1):
    r = user1.get("/users/me/change-requests")
    body = r.json()
    assert body["code"] == "00000"
    ids = [item["id"] for item in body["data"]["items"]]
    assert CHANGE_REQUEST_PENDING in ids


def test_list_change_requests_unauthenticated(anon):
    r = anon.get("/users/me/change-requests")
    assert r.json()["code"] == "A0100"
