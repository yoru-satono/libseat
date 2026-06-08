from helpers.constants import LIBRARY_ID, SEAT_AVAIL_Q1, SEAT_UNAVAIL


def test_list_seats_is_public(anon):
    r = anon.get("/seats", params={"libraryId": LIBRARY_ID})
    assert r.json()["code"] == "00000"


def test_list_seats_by_library(user1):
    r = user1.get("/seats", params={"libraryId": LIBRARY_ID})
    body = r.json()
    assert body["code"] == "00000"
    items = body["data"]["items"]
    assert len(items) >= 1
    for seat in items:
        assert seat["libraryId"] == LIBRARY_ID


def test_list_seats_excludes_unavailable(user1):
    r = user1.get("/seats", params={"libraryId": LIBRARY_ID, "status": "AVAILABLE"})
    ids = [s["id"] for s in r.json()["data"]["items"]]
    assert SEAT_UNAVAIL not in ids


def test_filter_by_area_quiet(user1):
    r = user1.get("/seats", params={"libraryId": LIBRARY_ID, "area": "QUIET"})
    body = r.json()
    assert body["code"] == "00000"
    assert all(s["area"] == "QUIET" for s in body["data"]["items"])


def test_filter_by_area_computer(user1):
    r = user1.get("/seats", params={"libraryId": LIBRARY_ID, "area": "COMPUTER"})
    body = r.json()
    assert body["code"] == "00000"
    assert all(s["area"] == "COMPUTER" for s in body["data"]["items"])


def test_get_seat_by_id(user1):
    r = user1.get(f"/seats/{SEAT_AVAIL_Q1}")
    body = r.json()
    assert body["code"] == "00000"
    assert body["data"]["seatNo"] == "2F-Q-001"
    assert body["data"]["area"] == "QUIET"
    assert body["data"]["floor"] == 2


def test_get_nonexistent_seat(user1):
    r = user1.get("/seats/00000000-0000-0000-0000-000000000000")
    assert r.json()["code"] != "00000"
