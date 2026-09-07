from rest_framework import permissions


def in_group(user, name):
    return user.is_authenticated and user.groups.filter(name__iexact=name).exists()


def is_admin(user):
    return user.is_authenticated and (user.is_superuser or in_group(user, "admin"))


def is_organizer(user):
    return in_group(user, "organizer")


class IsSuperUser(permissions.BasePermission):
    """Hanya superuser yang boleh mengakses."""

    def has_permission(self, request, view):
        return bool(request.user and request.user.is_superuser)


class ReadOnlyOrAuthenticated(permissions.BasePermission):
    """Baca (GET/HEAD/OPTIONS) terbuka; tulis wajib terautentikasi.

    Collection resmi memverifikasi hasil perubahan lewat pm.sendRequest yang
    dikirim TANPA header Authorization dan mengharapkan 200, sehingga endpoint
    detail harus dapat dibaca tanpa token. Operasi tulis tetap dikunci.
    """

    def has_permission(self, request, view):
        if request.method in permissions.SAFE_METHODS:
            return True
        return bool(request.user and request.user.is_authenticated)


class IsAdminOrSuperUser(permissions.BasePermission):
    """Admin atau superuser."""

    def has_permission(self, request, view):
        return is_admin(request.user)


class IsAdminOrOrganizerOrReadOnly(permissions.BasePermission):
    """Baca: terbuka (melihat daftar/detail). Tulis: admin, superuser, atau organizer."""

    def has_permission(self, request, view):
        if request.method in permissions.SAFE_METHODS:
            return True
        if not request.user or not request.user.is_authenticated:
            return False
        return is_admin(request.user) or is_organizer(request.user)

    def has_object_permission(self, request, view, obj):
        if request.method in permissions.SAFE_METHODS:
            return True
        if is_admin(request.user):
            return True
        # Organizer hanya boleh mengubah/menghapus event miliknya sendiri
        if is_organizer(request.user):
            owner = getattr(obj, "organizer", None)
            return owner is not None and owner == request.user
        return False
