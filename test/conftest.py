import pytest
import httpx

from helpers.constants import BASE_URL, PASSWORD, ADMIN_NO, USER1_NO, USER2_NO


def _login(user_no: str) -> httpx.Client:
    c = httpx.Client(base_url=BASE_URL)
    r = c.post("/auth/login", json={"userNo": user_no, "password": PASSWORD})
    assert r.status_code == 200, f"login failed for {user_no}: {r.text}"
    body = r.json()
    assert body["code"] == "00000", f"login error for {user_no}: {body}"
    token = body["data"]["accessToken"]
    c.headers["Authorization"] = f"Bearer {token}"
    return c


@pytest.fixture(scope="session")
def anon() -> httpx.Client:
    return httpx.Client(base_url=BASE_URL)


@pytest.fixture(scope="session")
def admin() -> httpx.Client:
    return _login(ADMIN_NO)


@pytest.fixture(scope="session")
def user1() -> httpx.Client:
    return _login(USER1_NO)


@pytest.fixture(scope="session")
def user2() -> httpx.Client:
    return _login(USER2_NO)
