import os
from urllib.parse import urlparse

from django.conf import settings


def cloudinary_configured():
    storage_backend = settings.STORAGES.get("default", {}).get("BACKEND", "")
    return (
        "cloudinary_storage" in storage_backend
        and bool(getattr(settings, "CLOUDINARY_CLOUD_NAME", None))
        and bool(getattr(settings, "CLOUDINARY_API_KEY", None))
        and bool(getattr(settings, "CLOUDINARY_API_SECRET", None))
    )


def upload_attachment(uploaded_file, folder):
    if not cloudinary_configured():
        return None

    import cloudinary.uploader

    try:
        uploaded_file.seek(0)
    except Exception:
        pass

    result = cloudinary.uploader.upload(
        uploaded_file,
        folder=folder,
        resource_type="auto",
        type="upload",
        use_filename=True,
        unique_filename=True,
    )

    secure_url = build_signed_attachment_url(
        result.get("public_id"),
        result.get("resource_type") or "raw",
    ) or result.get("secure_url")
    if not secure_url:
        raise ValueError("Cloudinary did not return a public file URL.")

    return {
        "url": secure_url,
        "name": os.path.basename(getattr(uploaded_file, "name", "") or "attachment"),
        "type": getattr(uploaded_file, "content_type", "") or "application/octet-stream",
        "public_id": result.get("public_id", ""),
        "resource_type": result.get("resource_type", ""),
    }


def build_signed_attachment_url(public_id, resource_type):
    if not public_id or not cloudinary_configured():
        return ""

    from cloudinary.utils import cloudinary_url

    url, _ = cloudinary_url(
        public_id,
        resource_type=resource_type or "raw",
        type="upload",
        secure=True,
        sign_url=True,
    )
    return url or ""


def signed_url_from_cloudinary_url(url):
    if not url or not cloudinary_configured():
        return url

    parsed = urlparse(url)
    parts = [part for part in parsed.path.split("/") if part]
    try:
        upload_index = parts.index("upload")
    except ValueError:
        return url

    if upload_index == 0:
        return url

    resource_type = parts[upload_index - 1]
    public_parts = parts[upload_index + 1:]
    if public_parts and public_parts[0].startswith("v") and public_parts[0][1:].isdigit():
        public_parts = public_parts[1:]
    public_id = "/".join(public_parts)
    return build_signed_attachment_url(public_id, resource_type) or url
