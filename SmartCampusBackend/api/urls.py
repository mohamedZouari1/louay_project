from django.urls import path
from . import views

urlpatterns = [
    # Auth
    path('auth/register/', views.register_view, name='register'),
    path('auth/login/', views.login_view, name='login'),
    path('auth/logout/', views.logout_view, name='logout'),

    # Profile
    path('profile/', views.profile_view, name='profile'),

    # Campus data
    path('locations/', views.locations_view, name='locations'),
    path('events/', views.events_view, name='events'),
    path('events/<int:pk>/register/', views.event_register_view, name='event-register'),
    path('stats/', views.stats_view, name='stats'),

    # Reports & Favorites
    path('reports/', views.reports_view, name='reports'),
    path('favorites/', views.favorites_view, name='favorites'),
    path('favorites/<int:pk>/', views.favorite_delete_view, name='favorite-delete'),

    # ── Social Hub ──────────────────────────
    path('social/feed/', views.social_feed_view, name='social-feed'),
    path('social/feed', views.social_feed_view), 
    path('social/posts/', views.social_create_post_view, name='social-create-post'),
    path('social/posts/text/', views.social_create_text_post_view, name='social-create-text-post'),
    path('social/posts/<int:pk>/like/', views.social_like_post_view, name='social-like-post'),
    path('social/users/search/', views.social_search_users_view, name='social-search-users'),
    path('social/users/<int:pk>/', views.social_public_profile_view, name='social-public-profile'),
    path('social/users/<int:pk>/follow/', views.social_follow_view, name='social-follow'),
    path('social/users/me/following/', views.get_following_view, name='get_following'),
    path('social/suggestions/', views.social_suggestions_view, name='social_suggestions'),
    path('social/posts/<int:pk>/comments/', views.social_comments_view, name='social-comments'),
    path('social/posts/<int:pk>/repost/', views.social_repost_view, name='social-repost'),

    # Chat
    path('chat/conversations/', views.chat_list_view, name='chat-list'),
    path('chat/messages/<int:pk>/', views.chat_messages_view, name='chat-messages'),
    path('profile/image/', views.update_profile_image, name='profile-image-update'),
    path('ping/', views.ping_view, name='ping'),
]
