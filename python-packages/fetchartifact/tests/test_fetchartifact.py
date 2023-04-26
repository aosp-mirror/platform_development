#
# Copyright (C) 2023 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""Tests for fetchartifact."""
from typing import cast

import pytest
from aiohttp import ClientResponseError, ClientSession
from aiohttp.test_utils import TestClient
from aiohttp.web import Application, Request, Response

from fetchartifact import fetch_artifact, fetch_artifact_chunked

TEST_BUILD_ID = "1234"
TEST_TARGET = "linux"
TEST_ARTIFACT_NAME = "output.zip"
TEST_DOWNLOAD_URL = (
    f"/android/internal/build/v3/builds/{TEST_BUILD_ID}/{TEST_TARGET}/"
    f"attempts/latest/artifacts/{TEST_ARTIFACT_NAME}/url"
)
TEST_RESPONSE = b"Hello, world!"


@pytest.fixture(name="android_ci_client")
async def fixture_android_ci_client(aiohttp_client: type[TestClient]) -> TestClient:
    """Fixture for mocking the Android CI APIs."""

    async def download(_request: Request) -> Response:
        return Response(text=TEST_RESPONSE.decode("utf-8"))

    app = Application()
    app.router.add_get(TEST_DOWNLOAD_URL, download)
    return await aiohttp_client(app)  # type: ignore


async def test_fetch_artifact(android_ci_client: TestClient) -> None:
    """Tests that the download URL is queried."""
    assert TEST_RESPONSE == await fetch_artifact(
        TEST_TARGET,
        TEST_BUILD_ID,
        TEST_ARTIFACT_NAME,
        cast(ClientSession, android_ci_client),
        query_url_base="",
    )


async def test_fetch_artifact_chunked(android_ci_client: TestClient) -> None:
    """Tests that the full file contents are downloaded."""
    assert [c.encode("utf-8") for c in TEST_RESPONSE.decode("utf-8")] == [
        chunk
        async for chunk in fetch_artifact_chunked(
            TEST_TARGET,
            TEST_BUILD_ID,
            TEST_ARTIFACT_NAME,
            cast(ClientSession, android_ci_client),
            chunk_size=1,
            query_url_base="",
        )
    ]


async def test_failure_raises(android_ci_client: TestClient) -> None:
    """Tests that fetch failure raises an exception."""
    with pytest.raises(ClientResponseError):
        await fetch_artifact(
            TEST_TARGET,
            TEST_BUILD_ID,
            TEST_ARTIFACT_NAME,
            cast(ClientSession, android_ci_client),
            query_url_base="/bad",
        )

    with pytest.raises(ClientResponseError):
        async for _chunk in fetch_artifact_chunked(
            TEST_TARGET,
            TEST_BUILD_ID,
            TEST_ARTIFACT_NAME,
            cast(ClientSession, android_ci_client),
            query_url_base="/bad",
        ):
            pass


@pytest.mark.requires_network
async def test_real_artifact() -> None:
    """Tests with a real artifact. Requires an internet connection."""
    async with ClientSession() as session:
        contents = await fetch_artifact("linux", "9945621", "logs/SUCCEEDED", session)
        assert contents == b"1681499053\n"
