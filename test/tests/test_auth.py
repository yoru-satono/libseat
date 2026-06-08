import pytest
import httpx

from helpers.constants import (
    BASE_URL, PASSWORD,
    ADMIN_NO, USER1_NO, USER3_NO, USER4_NO, USER5_NO,
)


@pytest.fixture(scope="module")
def client():
    return httpx.Client(base_url=BASE_URL)


def test_login_success(client):
    r = client.post("/auth/login", json={"userNo": USER1_NO, "password": PASSWORD})
    body = r.json()
    assert body["code"] == "00000"
    assert "accessToken" in body["data"]
    assert "refreshToken" in body["data"]


def test_login_wrong_password(client):
    r = client.post("/auth/login", json={"userNo": USER1_NO, "password": "wrong"})
    assert r.json()["code"] == "A0600"


def test_login_inactive_account(client):
    r = client.post("/auth/login", json={"userNo": USER3_NO, "password": PASSWORD})
    assert r.json()["code"] == "A0603"


def test_login_locked_account(client):
    r = client.post("/auth/login", json={"userNo": USER4_NO, "password": PASSWORD})
    assert r.json()["code"] == "A0601"


def test_login_suspended_account(client):
    r = client.post("/auth/login", json={"userNo": USER5_NO, "password": PASSWORD})
    assert r.json()["code"] == "A0602"


def test_login_missing_fields(client):
    r = client.post("/auth/login", json={"userNo": USER1_NO})
    assert r.json()["code"] != "00000"


def test_refresh_token(client):
    login = client.post("/auth/login", json={"userNo": USER1_NO, "password": PASSWORD})
    refresh_token = login.json()["data"]["refreshToken"]

    r = client.post("/auth/refresh", json={"refreshToken": refresh_token})
    body = r.json()
    assert body["code"] == "00000"
    assert "accessToken" in body["data"]


def test_logout(client):
    login = client.post("/auth/login", json={"userNo": ADMIN_NO, "password": PASSWORD})
    access_token = login.json()["data"]["accessToken"]

    r = client.post("/auth/logout", headers={"Authorization": f"Bearer {access_token}"})
    assert r.json()["code"] == "00000"
