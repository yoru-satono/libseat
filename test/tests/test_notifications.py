from helpers.constants import NOTIFICATION_USER1_UNREAD


def test_list_notifications_unauthenticated(anon):
    r = anon.get("/notifications")
    assert r.json()["code"] == "A0100"


def test_list_notifications(user1):
    r = user1.get("/notifications")
    body = r.json()
    assert body["code"] == "00000"
    for key in ("items", "total"):
        assert key in body["data"]


def test_list_notifications_unread_only(user1):
    r = user1.get("/notifications", params={"isRead": "false"})
    body = r.json()
    assert body["code"] == "00000"
    for item in body["data"]["items"]:
        assert item["isRead"] is False


def test_unread_count(user1):
    r = user1.get("/notifications/unread-count")
    body = r.json()
    assert body["code"] == "00000"
    assert isinstance(body["data"]["count"], int)
    assert body["data"]["count"] >= 0


def test_mark_one_notification_read(user1):
    r = user1.patch(f"/notifications/{NOTIFICATION_USER1_UNREAD}/read")
    assert r.json()["code"] == "00000"


def test_mark_all_notifications_read(user1):
    r = user1.patch("/notifications/read-all")
    assert r.json()["code"] == "00000"

    count = user1.get("/notifications/unread-count").json()["data"]["count"]
    assert count == 0
