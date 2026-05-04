from django.urls import path
from . import views

urlpatterns = [
    path('auth/register/', views.register_view, name='register'),
    path('auth/login/', views.login_view, name='login'),
    path('auth/logout/', views.logout_view, name='logout'),
    path('profile/', views.profile_view, name='profile'),
    path('locations/', views.locations_view, name='locations'),
    path('events/', views.events_view, name='events'),
    path('stats/', views.stats_view, name='stats'),
    path('reports/', views.reports_view, name='reports'),
    path('favorites/', views.favorites_view, name='favorites'),
    path('favorites/<int:pk>/', views.favorite_delete_view, name='favorite-delete'),
]
