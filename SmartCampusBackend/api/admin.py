from django.contrib import admin
from .models import (
    UserProfile, CampusLocation, Event, Report, Favorite, CampusStat,
    Post, PostLike, UserFollow,
)


@admin.register(UserProfile)
class UserProfileAdmin(admin.ModelAdmin):
    list_display = ['user', 'university', 'role', 'student_id', 'joined_date']
    list_filter = ['role', 'university']
    search_fields = ['user__username', 'user__email', 'user__first_name', 'user__last_name']
    # Admins can promote users to 'org' or 'admin' role here
    fields = ['user', 'university', 'student_id', 'role', 'bio', 'avatar_color', 'joined_date']
    readonly_fields = ['joined_date']


@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    list_display = ['id', 'author', 'post_type', 'content_preview', 'likes_count', 'created_at']
    list_filter = ['post_type', 'created_at']
    search_fields = ['author__username', 'content']

    def content_preview(self, obj):
        return obj.content[:60] + ('…' if len(obj.content) > 60 else '')
    content_preview.short_description = 'Content'

    def likes_count(self, obj):
        return obj.likes.count()
    likes_count.short_description = 'Likes'


@admin.register(PostLike)
class PostLikeAdmin(admin.ModelAdmin):
    list_display = ['user', 'post', 'created_at']


@admin.register(UserFollow)
class UserFollowAdmin(admin.ModelAdmin):
    list_display = ['follower', 'following', 'created_at']


admin.site.register(CampusLocation)
admin.site.register(Event)
admin.site.register(Report)
admin.site.register(Favorite)
admin.site.register(CampusStat)
