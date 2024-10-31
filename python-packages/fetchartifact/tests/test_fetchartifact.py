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

from fetchartifact import ArtifactDownloader, fetch_artifact, fetch_artifact_chunked

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


class TestDownloader(ArtifactDownloader):
    """Downloader which tracks calls to on_artifact_size and after_chunk."""

    def __init__(self, target: str, build_id: str, artifact_name: str) -> None:
        super().__init__(target, build_id, artifact_name, query_url_base="")
        self.reported_content_length: int | None = None
        self.reported_chunk_sizes: list[int] = []

    def on_artifact_size(self, size: int) -> None:
        super().on_artifact_size(size)
        assert self.reported_content_length is None
        self.reported_content_length = size

    def after_chunk(self, size: int) -> None:
        super().after_chunk(size)
        self.reported_chunk_sizes.append(size)


async def test_downloader_progress_reports(android_ci_client: TestClient) -> None:
    """Tests that progress is reported when using ArtifactDownloader."""
    downloader = TestDownloader(TEST_TARGET, TEST_BUILD_ID, TEST_ARTIFACT_NAME)

    assert [b"Hell", b"o, w", b"orld", b"!"] == [
        chunk
        async for chunk in downloader.download(
            cast(ClientSession, android_ci_client), chunk_size=4
        )
    ]
    assert downloader.reported_content_length == len(TEST_RESPONSE.decode("utf-8"))
    assert downloader.reported_chunk_sizes == [4, 4, 4, 1]


@pytest.mark.requires_network
async def test_real_artifact() -> None:
    """Tests with a real artifact. Requires an internet connection."""
    async with ClientSession() as session:
        contents = await fetch_artifact("linux", "9945621", "logs/SUCCEEDED", session)
        assert contents == b"1681499053\n"
