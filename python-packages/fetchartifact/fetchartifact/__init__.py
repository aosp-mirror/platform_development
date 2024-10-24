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
"""A Python interface to https://android.googlesource.com/tools/fetch_artifact/."""
import logging
import urllib
from collections.abc import AsyncIterable
from logging import Logger
from typing import cast

from aiohttp import ClientSession

_DEFAULT_QUERY_URL_BASE = "https://androidbuildinternal.googleapis.com"
DEFAULT_CHUNK_SIZE = 16 * 1024 * 1024


def _logger() -> Logger:
    return logging.getLogger("fetchartifact")


def _make_download_url(
    target: str,
    build_id: str,
    artifact_name: str,
    query_url_base: str,
) -> str:
    """Constructs the download URL.

    Args:
        target: Name of the build target from which to fetch the artifact.
        build_id: ID of the build from which to fetch the artifact.
        artifact_name: Name of the artifact to fetch.

    Returns:
        URL for the given artifact.
    """
    # The Android build API does not handle / in artifact names, but urllib.parse.quote
    # thinks those are safe by default. We need to escape them.
    artifact_name = urllib.parse.quote(artifact_name, safe="")
    return (
        f"{query_url_base}/android/internal/build/v3/builds/{build_id}/{target}/"
        f"attempts/latest/artifacts/{artifact_name}/url"
    )


async def fetch_artifact(
    target: str,
    build_id: str,
    artifact_name: str,
    session: ClientSession,
    query_url_base: str = _DEFAULT_QUERY_URL_BASE,
) -> bytes:
    """Fetches an artifact from the build server.

    Args:
        target: Name of the build target from which to fetch the artifact.
        build_id: ID of the build from which to fetch the artifact.
        artifact_name: Name of the artifact to fetch.
        session: The aiohttp ClientSession to use. If omitted, one will be created and
            destroyed for every call.
        query_url_base: The base of the endpoint used for querying download URLs. Uses
            the android build service by default, but can be replaced for testing.

    Returns:
        The bytes of the downloaded artifact.
    """
    download_url = _make_download_url(target, build_id, artifact_name, query_url_base)
    _logger().debug("Beginning download from %s", download_url)
    async with session.get(download_url) as response:
        response.raise_for_status()
        return await response.read()


async def fetch_artifact_chunked(
    target: str,
    build_id: str,
    artifact_name: str,
    session: ClientSession,
    chunk_size: int = DEFAULT_CHUNK_SIZE,
    query_url_base: str = _DEFAULT_QUERY_URL_BASE,
) -> AsyncIterable[bytes]:
    """Fetches an artifact from the build server.

    Args:
        target: Name of the build target from which to fetch the artifact.
        build_id: ID of the build from which to fetch the artifact.
        artifact_name: Name of the artifact to fetch.
        session: The aiohttp ClientSession to use. If omitted, one will be created and
            destroyed for every call.
        query_url_base: The base of the endpoint used for querying download URLs. Uses
            the android build service by default, but can be replaced for testing.

    Returns:
        Async iterable bytes of the artifact contents.
    """
    downloader = ArtifactDownloader(target, build_id, artifact_name, query_url_base)
    async for chunk in downloader.download(session, chunk_size):
        yield chunk


class ArtifactDownloader:
    """Similar to fetch_artifact_chunked but can be subclassed to report progress."""

    def __init__(
        self,
        target: str,
        build_id: str,
        artifact_name: str,
        query_url_base: str = _DEFAULT_QUERY_URL_BASE,
    ) -> None:
        self.target = target
        self.build_id = build_id
        self.artifact_name = artifact_name
        self.query_url_base = query_url_base

    async def download(
        self, session: ClientSession, chunk_size: int = DEFAULT_CHUNK_SIZE
    ) -> AsyncIterable[bytes]:
        """Fetches an artifact from the build server.

        Once the Content-Length of the artifact is known, on_artifact_size will be
        called. If no Content-Length header is present, on_artifact_size will not be
        called.

        After each chunk is yielded, after_chunk will be called.

        Args:
            session: The aiohttp ClientSession to use. If omitted, one will be created
                and destroyed for every call.
            chunk_size: The number of bytes to attempt to download before yielding and
                reporting progress.

        Returns:
            Async iterable bytes of the artifact contents.
        """
        download_url = _make_download_url(
            self.target, self.build_id, self.artifact_name, self.query_url_base
        )
        _logger().debug("Beginning download from %s", download_url)
        async with session.get(download_url) as response:
            response.raise_for_status()
            try:
                self.on_artifact_size(int(response.headers["Content-Length"]))
            except KeyError:
                pass

            async for chunk in response.content.iter_chunked(chunk_size):
                yield chunk
                self.after_chunk(len(chunk))

    def on_artifact_size(self, size: int) -> None:
        """Called once the artifact's Content-Length is known.

        This can be overridden in a subclass to enable progress reporting.

        If the artifact headers do not report the Content-Length, this function will not
        be called.

        Args:
            size: The Content-Length of the artifact.
        """

    def after_chunk(self, size: int) -> None:
        """Called after each chunk is yielded.

        This can be overridden in a subclass to enable progress reporting.

        Args:
            size: The size of the chunk.
        """
