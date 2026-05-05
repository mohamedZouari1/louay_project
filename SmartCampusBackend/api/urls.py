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
]
