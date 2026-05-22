import os

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

    result = cloudinary.uploader.upload(
        uploaded_file,
        folder=folder,
        resource_type="auto",
        type="upload",
        access_mode="public",
        use_filename=True,
        unique_filename=True,
    )

    secure_url = result.get("secure_url")
    if not secure_url:
        raise ValueError("Cloudinary did not return a public file URL.")

    return {
        "url": secure_url,
        "name": os.path.basename(getattr(uploaded_file, "name", "") or "attachment"),
        "type": getattr(uploaded_file, "content_type", "") or "application/octet-stream",
    }
